package com.jua.leaderboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PodiumRateDTO {
    private Integer playerId;
    private String playerName;
    private Long attended;
    private Long podiumFinishes;
    private Double podiumRate;
}