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

    public DiscordInteractionController(
        DiscordSignatureVerifier signatureVerifier,
        ObjectMapper objectMapper,
        CommandLogRepository commandLogRepository) {

        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
        this.commandLogRepository = commandLogRepository;
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

            System.out.println("INTERACTION: " + body);

            int type = interaction.get("type").asInt();

            // 3. Discord PING
            if (type == 1) {
                return ResponseEntity.ok(
                        "{\"type\":1}"
                );
            }

            // 4. Discord slash command
            if (type == 2) {

                String commandName =
                        interaction
                                .get("data")
                                .get("name")
                                .asText();
                String interactionId = interaction.get("id").asText();
String userId = interaction.get("member").get("user").get("id").asText();
String channelId = interaction.get("channel_id").asText();

if (commandLogRepository.existsByInteractionId(interactionId)) {
    return ResponseEntity.ok(
            "{\"type\":4,\"data\":{\"content\":\"Already processed\"}}"
    );
}

CommandLog log = new CommandLog();
log.setInteractionId(interactionId);
log.setCommandName(commandName);
log.setUserId(userId);
log.setChannelId(channelId);
log.setCreatedAt(java.time.Instant.now());

if (commandName.equals("report")) {
    String reportText = interaction
            .get("data")
            .get("options")
            .get(0)
            .get("value")
            .asText();

    log.setText(reportText);
}

CommandLog savedLog = commandLogRepository.save(log);

System.out.println(
        "LOG SAVED: id=" + savedLog.getId()
        + ", command=" + savedLog.getCommandName()
        + ", user=" + savedLog.getUserId()
);


                if (commandName.equals("status")) {

                    return ResponseEntity.ok(
                            "{\"type\":4,\"data\":{\"content\":\"Bot is running! ✅\"}}"
                    );
                }

                if (commandName.equals("report")) {

                    String reportText =
                            interaction
                                    .get("data")
                                    .get("options")
                                    .get(0)
                                    .get("value")
                                    .asText();

                    return ResponseEntity.ok(
                            "{\"type\":4,\"data\":{\"content\":\"Report received: "
                                    + reportText
                                    + "\"}}"
                    );
                }

                return ResponseEntity.ok(
                        "{\"type\":4,\"data\":{\"content\":\"Unknown command\"}}"
                );
            }

            return ResponseEntity.badRequest()
                    .body("{\"error\":\"Unsupported interaction type\"}");

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("{\"error\":\"Invalid JSON\"}");
        }
    }
}