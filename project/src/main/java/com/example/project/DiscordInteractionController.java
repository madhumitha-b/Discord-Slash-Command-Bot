package com.example.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @PostMapping("/interactions")
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
                    .body("Invalid signature");
        }

        try {
            // 2. Convert JSON body into a JsonNode
            JsonNode interaction = objectMapper.readTree(body);

            // 3. Get interaction type
            int type = interaction.get("type").asInt();

            // 4. Handle Discord PING
            if (type == 1) {
                return ResponseEntity.ok(
                        "{\"type\":1}"
                );
            }

            // Other interaction types will be handled later
            return ResponseEntity.ok("received");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Invalid JSON");
        }
    }
}