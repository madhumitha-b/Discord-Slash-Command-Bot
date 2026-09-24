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

    public DiscordInteractionController(
            DiscordSignatureVerifier signatureVerifier,
            ObjectMapper objectMapper) {

        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
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