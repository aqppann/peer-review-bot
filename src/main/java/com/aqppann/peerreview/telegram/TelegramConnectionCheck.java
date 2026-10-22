package com.aqppann.peerreview.telegram;

import com.aqppann.peerreview.config.TelegramBotProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import com.aqppann.peerreview.telegram.dto.TelegramUpdatesResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class TelegramConnectionCheck implements CommandLineRunner {
    private final TelegramBotProperties properties;
    private final JsonMapper jsonMapper;

    public TelegramConnectionCheck(TelegramBotProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void run(String... args) {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://api.telegram.org/bot"
                                    + properties.token()
                                    + "/getUpdates?timeout=20&limit=1"
                    ))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            String responseBody = response.body();
            if (response.statusCode() != 200) {
                System.err.println(
                        "Telegram request failed: HTTP " + response.statusCode()
                );
                return;
            }

            TelegramUpdatesResponse updatesResponse = jsonMapper.readValue(
                    responseBody,
                    TelegramUpdatesResponse.class
            );

            if (!updatesResponse.ok() || updatesResponse.result() == null) {
                System.err.println("Telegram returned an unsuccessful response.");
                return;
            }

            for (var update : updatesResponse.result()) {
                var message = update.message();

                if (message == null || message.text() == null) {
                    continue;
                }

                if ("/start".equals(message.text())) {
                    System.out.println("Received /start command.");
                }
            }

            System.out.println(
                    "Telegram getUpdates: HTTP " + response.statusCode()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Telegram connection check interrupted.");
        } catch (IOException exception) {
            System.err.println("Could not connect to Telegram.");
        }
    }
}