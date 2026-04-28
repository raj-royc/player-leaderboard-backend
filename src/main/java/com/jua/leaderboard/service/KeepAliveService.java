package com.jua.leaderboard.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KeepAliveService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedDelay = 600000) // every 10 minutes
    public void keepAlive() {
        try {
            restTemplate.getForObject(
                    "https://player-leaderboard-backend.onrender.com/api/players",
                    String.class
            );
        } catch (Exception ignored) {}
    }
}