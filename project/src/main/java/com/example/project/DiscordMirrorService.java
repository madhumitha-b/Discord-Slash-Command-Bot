package com.example.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class DiscordMirrorService {

    private final ObjectMapper objectMapper;
    private final String webhookUrl;
    private final HttpClient httpClient;

    public DiscordMirrorService(
            ObjectMapper objectMapper,
            @Value("${discord.mirror.webhook-url}") String webhookUrl) {

        this.objectMapper = objectMapper;
        this.webhookUrl = webhookUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Async
    public void sendToMirrorChannel(String message) {

        try {
            String json = objectMapper
                    .createObjectNode()
                    .put("text", message)
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println(
                    "SLACK WEBHOOK STATUS: " + response.statusCode()
            );

            System.out.println(
                    "SLACK WEBHOOK RESPONSE: " + response.body()
            );

        } catch (Exception e) {
            System.out.println(
                    "SLACK WEBHOOK FAILED: " + e.getMessage()
            );
        }
    }
}