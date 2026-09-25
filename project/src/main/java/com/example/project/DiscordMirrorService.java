package com.example.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class DiscordMirrorService {

    private final ObjectMapper objectMapper;
    private final String botToken;
    private final String mirrorChannelId;

    public DiscordMirrorService(
            ObjectMapper objectMapper,
            @Value("${discord.bot.token}") String botToken,
            @Value("${discord.mirror.channel.id}") String mirrorChannelId) {

        this.objectMapper = objectMapper;
        this.botToken = botToken;
        this.mirrorChannelId = mirrorChannelId;
    }

public void sendToMirrorChannel(String message) {

    try {
        String json = objectMapper
                .createObjectNode()
                .put("content", message)
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://discord.com/api/v10/channels/"
                                + mirrorChannelId
                                + "/messages"
                ))
                .header("Authorization", "Bot " + botToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("MIRROR STATUS: " + response.statusCode());
        System.out.println("MIRROR RESPONSE: " + response.body());

    } catch (Exception e) {
        System.out.println("MIRROR FAILED: " + e.getMessage());
    }
}
}