package com.aqppann.peerreview.telegram;

import com.aqppann.peerreview.config.TelegramBotProperties;
import com.aqppann.peerreview.telegram.dto.TelegramUpdatesResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

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
    private final TelegramClient telegramClient;

    public TelegramConnectionCheck(
            TelegramBotProperties properties,
            JsonMapper jsonMapper,
            TelegramClient telegramClient
    ) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.telegramClient = telegramClient;
    }

    @Override
    public void run(String... args) {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {

            long offset = 0;

            while (!Thread.currentThread().isInterrupted()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "https://api.telegram.org/bot"
                                        + properties.token()
                                        + "/getUpdates?timeout=20&limit=1&offset="
                                        + offset
                        ))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() != 200) {
                    System.err.println(
                            "Telegram getUpdates failed: HTTP "
                                    + response.statusCode()
                    );
                    return;
                }

                TelegramUpdatesResponse updatesResponse =
                        jsonMapper.readValue(
                                response.body(),
                                TelegramUpdatesResponse.class
                        );

                if (!updatesResponse.ok()
                        || updatesResponse.result() == null) {
                    System.err.println(
                            "Telegram returned an unsuccessful response."
                    );
                    return;
                }

                for (var update : updatesResponse.result()) {
                    var message = update.message();

                    if (message != null
                            && message.chat() != null
                            && "/start".equals(message.text())) {

                        boolean sent = telegramClient.sendMessage(
                                client,
                                message.chat().id(),
                                "Welcome to Peer Review Bot!"
                        );

                        if (!sent) {
                            return;
                        }
                    }

                    offset = update.updateId() + 1;
                }
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Telegram polling interrupted.");
        } catch (IOException exception) {
            System.err.println("Could not connect to Telegram.");
        }
    }
}