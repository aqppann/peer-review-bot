package com.aqppann.peerreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class PeerReviewBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(PeerReviewBotApplication.class, args);
    }
}
