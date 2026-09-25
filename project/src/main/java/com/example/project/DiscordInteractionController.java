package com.example.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/discord")
public class DiscordInteractionController {

    private static final long MAX_TIMESTAMP_AGE_SECONDS = 300;

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
            return ResponseEntity.status(401)
                    .body("{\"error\":\"Invalid signature\"}");
        }

        // 2. Check timestamp freshness
        long requestTimestamp;

        try {
            requestTimestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(401)
                    .body("{\"error\":\"Invalid timestamp\"}");
        }

        long currentTimestamp = Instant.now().getEpochSecond();

        if (Math.abs(currentTimestamp - requestTimestamp)
                > MAX_TIMESTAMP_AGE_SECONDS) {

            return ResponseEntity.status(401)
                    .body("{\"error\":\"Request timestamp expired\"}");
        }

        try {

            // 3. Convert JSON request into JsonNode
            JsonNode interaction = objectMapper.readTree(body);

            int type = interaction.get("type").asInt();

            // 4. Discord PING
            if (type == 1) {
                return ResponseEntity.ok("{\"type\":1}");
            }

            // 5. Discord slash command
            if (type == 2) {

                String commandName = interaction
                        .get("data")
                        .get("name")
                        .asText();

                // 6. Get command configuration from PostgreSQL
                CommandConfig config = commandConfigRepository
                        .findByCommandName(commandName)
                        .orElse(null);

                if (config == null) {
                    return ResponseEntity.ok(
                            "{\"type\":4,\"data\":{\"content\":\"Command is not configured\"}}"
                    );
                }

                // 7. Check whether command is enabled
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

                // 8. Prevent duplicate processing
                if (commandLogRepository.existsByInteractionId(interactionId)) {
                    return ResponseEntity.ok(
                            "{\"type\":4,\"data\":{\"content\":\"Already processed\"}}"
                    );
                }

                // 9. Create command log
                CommandLog log = new CommandLog();

                log.setInteractionId(interactionId);
                log.setCommandName(commandName);
                log.setUserId(userId);
                log.setChannelId(channelId);
                log.setCreatedAt(Instant.now());

                String reportText = null;

                // 10. Extract report text
                if (commandName.equals("report")) {

                    JsonNode options = interaction
                            .get("data")
                            .get("options");

                    if (options != null && options.isArray() && !options.isEmpty()) {

                        reportText = options
                                .get(0)
                                .get("value")
                                .asText();

                        log.setText(reportText);

                    } else {

                        log.setActionTaken(
                                "Command received but report text was missing"
                        );

                        commandLogRepository.save(log);

                        return ResponseEntity.ok(
                                "{\"type\":4,\"data\":{\"content\":\"Report text is required\"}}"
                        );
                    }
                }

                // 11. STATUS
                if (commandName.equals("status")) {

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

                    // Mirror to Slack only if enabled
                    if (config.isMirrorEnabled()) {

                        discordMirrorService.sendToMirrorChannel(
                                config.getResponseMessage(),
                                savedLog.getId()
                        );
                    }

                    return ResponseEntity.ok(
                            "{\"type\":4,\"data\":{\"content\":\""
                                    + config.getResponseMessage()
                                    + "\"}}"
                    );
                }

                // 12. REPORT
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

                    // Mirror to Slack only if enabled
                    if (config.isMirrorEnabled()) {

                        discordMirrorService.sendToMirrorChannel(
                        config.getResponseMessage()
                        + ": "
                        + reportText,
                        savedLog.getId()
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

                // 13. Unknown command
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