package com.example.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discord")
public class DiscordInteractionController {

    private final DiscordSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final CommandLogRepository commandLogRepository;
    private final DiscordMirrorService discordMirrorService;
    private final CommandConfigRepository commandConfigRepository;

    public DiscordInteractionController(
            DiscordSignatureVerifier signatureVerifier,
            ObjectMapper objectMapper,
            CommandLogRepository commandLogRepository,
            DiscordMirrorService discordMirrorService,
            CommandConfigRepository commandConfigRepository) {

        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
        this.commandLogRepository = commandLogRepository;
        this.discordMirrorService = discordMirrorService;
        this.commandConfigRepository = commandConfigRepository;
    }

    @PostMapping(
            value = "/interactions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> handleInteraction(
            @RequestHeader("X-Signature-Ed25519") String signature,
            @RequestHeader("X-Signature-Timestamp") String timestamp,
            @RequestBody String body) {

        // 1. Verify Discord signature
        boolean valid = signatureVerifier.verify(
                body,
                signature,
                timestamp
        );

        if (!valid) {
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"Invalid signature\"}");
        }

        try {

            // 2. Convert JSON request into JsonNode
            JsonNode interaction = objectMapper.readTree(body);

            int type = interaction.get("type").asInt();

            // 3. Discord PING
            if (type == 1) {
                return ResponseEntity.ok("{\"type\":1}");
            }

            // 4. Discord slash command
            if (type == 2) {

                String commandName = interaction
                        .get("data")
                        .get("name")
                        .asText();

                // 5. Get command configuration from PostgreSQL
                CommandConfig config = commandConfigRepository
                        .findByCommandName(commandName)
                        .orElse(null);

                if (config == null) {
                    return ResponseEntity.ok(
                            "{\"type\":4,\"data\":{\"content\":\"Command is not configured\"}}"
                    );
                }

                // 6. Check whether command is enabled
                if (!config.isEnabled()) {
                    return ResponseEntity.ok(
                            "{\"type\":4,\"data\":{\"content\":\"This command is currently disabled\"}}"
                    );
                }

                String interactionId = interaction
                        .get("id")
                        .asText();

                String userId = interaction
                        .get("member")
                        .get("user")
                        .get("id")
                        .asText();

                String channelId = interaction
                        .get("channel_id")
                        .asText();

                // 7. Prevent duplicate processing
                if (commandLogRepository.existsByInteractionId(interactionId)) {
                    return ResponseEntity.ok(
                            "{\"type\":4,\"data\":{\"content\":\"Already processed\"}}"
                    );
                }

                // 8. Create command log
                CommandLog log = new CommandLog();

                log.setInteractionId(interactionId);
                log.setCommandName(commandName);
                log.setUserId(userId);
                log.setChannelId(channelId);
                log.setCreatedAt(java.time.Instant.now());

                String reportText = null;

                // 9. Extract report text
                if (commandName.equals("report")) {

                    reportText = interaction
                            .get("data")
                            .get("options")
                            .get(0)
                            .get("value")
                            .asText();

                    log.setText(reportText);
                }

                // 10. STATUS
                if (commandName.equals("status")) {

                log.setActionTaken(
                        config.isMirrorEnabled()
                                ? "Command saved to DB; Discord response sent; Slack mirror triggered"
                                : "Command saved to DB; Discord response sent; Slack mirror disabled"
                );

                CommandLog savedLog = commandLogRepository.save(log);

                System.out.println(
                        "LOG SAVED: id=" + savedLog.getId()
                                + ", command=" + savedLog.getCommandName()
                                + ", user=" + savedLog.getUserId()
                );

                if (config.isMirrorEnabled()) {
                        discordMirrorService.sendToMirrorChannel(
                                config.getResponseMessage()
                        );
                }

                return ResponseEntity.ok(
                        "{\"type\":4,\"data\":{\"content\":\""
                                + config.getResponseMessage()
                                + "\"}}"
                );
                }

                // 11. REPORT
                if (commandName.equals("report")) {

                    log.setActionTaken(
                            config.isMirrorEnabled()
                                    ? "Command saved to DB; Discord response sent; Slack mirror triggered"
                                    : "Command saved to DB; Discord response sent; Slack mirror disabled"
                    );

                    CommandLog savedLog =
                            commandLogRepository.save(log);

                    System.out.println(
                            "LOG SAVED: id=" + savedLog.getId()
                                    + ", command=" + savedLog.getCommandName()
                                    + ", user=" + savedLog.getUserId()
                    );

                    // Start Slack mirror only when enabled
                    if (config.isMirrorEnabled()) {

                        discordMirrorService.sendToMirrorChannel(
                                config.getResponseMessage()
                                        + ": "
                                        + reportText
                        );
                    }

                    return ResponseEntity.ok(
                            "{\"type\":4,\"data\":{\"content\":\""
                                    + config.getResponseMessage()
                                    + ": "
                                    + reportText
                                    + "\"}}"
                    );
                }

                // 12. Unknown command
                log.setActionTaken(
                        "Command saved to DB; unknown command response sent"
                );

                CommandLog savedLog =
                        commandLogRepository.save(log);

                System.out.println(
                        "LOG SAVED: id=" + savedLog.getId()
                                + ", command=" + savedLog.getCommandName()
                                + ", user=" + savedLog.getUserId()
                );

                return ResponseEntity.ok(
                        "{\"type\":4,\"data\":{\"content\":\"Unknown command\"}}"
                );
            }

            return ResponseEntity.badRequest()
                    .body("{\"error\":\"Unsupported interaction type\"}");

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.badRequest()
                    .body("{\"error\":\"Invalid JSON\"}");
        }
    }
}