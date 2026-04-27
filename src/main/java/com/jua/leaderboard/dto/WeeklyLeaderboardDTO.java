package com.jua.leaderboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyLeaderboardDTO {

    private Integer rank;
    private Integer playerId;
    private String playerName;
    private Long totalPoints;
}