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

    private static final int MAX_ATTEMPTS = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 2000;

    private final ObjectMapper objectMapper;
    private final String webhookUrl;
    private final HttpClient httpClient;
    private final CommandLogRepository commandLogRepository;

    public DiscordMirrorService(
            ObjectMapper objectMapper,
            @Value("${discord.mirror.webhook-url}") String webhookUrl,
            CommandLogRepository commandLogRepository) {

        this.objectMapper = objectMapper;
        this.webhookUrl = webhookUrl;
        this.commandLogRepository = commandLogRepository;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Async
    public void sendToMirrorChannel(String message, Long logId) {

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

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

                HttpResponse<String> response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                int statusCode = response.statusCode();

                System.out.println(
                        "SLACK MIRROR ATTEMPT "
                                + attempt
                                + " STATUS: "
                                + statusCode
                );

                // Success
                if (statusCode >= 200 && statusCode < 300) {

                    updateAction(
                            logId,
                            "Command saved to DB; Discord response sent; Slack mirror sent"
                    );

                    return;
                }

                // Retry only for rate limits or server-side failures
                if (statusCode == 429 || statusCode >= 500) {

                    if (attempt < MAX_ATTEMPTS) {

                        long delay =
                                getRetryDelay(response);

                        System.out.println(
                                "Retrying Slack mirror in "
                                        + delay
                                        + " ms"
                        );

                        Thread.sleep(delay);

                        continue;
                    }

                    updateAction(
                            logId,
                            "Command saved to DB; Discord response sent; Slack mirror failed after "
                                    + MAX_ATTEMPTS
                                    + " attempts (HTTP "
                                    + statusCode
                                    + ")"
                    );

                    return;
                }

                // Non-retryable error
                updateAction(
                        logId,
                        "Command saved to DB; Discord response sent; Slack mirror failed (HTTP "
                                + statusCode
                                + ")"
                );

                return;

            } catch (Exception e) {

                System.out.println(
                        "SLACK MIRROR ATTEMPT "
                                + attempt
                                + " FAILED: "
                                + e.getMessage()
                );

                if (attempt < MAX_ATTEMPTS) {

                    try {
                        Thread.sleep(DEFAULT_RETRY_DELAY_MS);
                    } catch (InterruptedException interruptedException) {

                        Thread.currentThread().interrupt();

                        updateAction(
                                logId,
                                "Command saved to DB; Discord response sent; Slack mirror interrupted"
                        );

                        return;
                    }

                } else {

                    updateAction(
                            logId,
                            "Command saved to DB; Discord response sent; Slack mirror failed after "
                                    + MAX_ATTEMPTS
                                    + " attempts"
                    );
                }
            }
        }
    }

    private long getRetryDelay(HttpResponse<String> response) {

        String retryAfter = response
                .headers()
                .firstValue("Retry-After")
                .orElse(null);

        if (retryAfter != null) {

            try {

                long seconds = Long.parseLong(retryAfter);

                return seconds * 1000;

            } catch (NumberFormatException ignored) {
                // Use default delay below
            }
        }

        return DEFAULT_RETRY_DELAY_MS;
    }

    private void updateAction(Long logId, String action) {

        commandLogRepository.findById(logId)
                .ifPresent(log -> {

                    log.setActionTaken(action);

                    commandLogRepository.save(log);

                });
    }
}