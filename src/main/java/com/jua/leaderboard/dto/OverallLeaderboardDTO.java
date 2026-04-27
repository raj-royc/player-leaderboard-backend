package com.jua.leaderboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OverallLeaderboardDTO {

    private Integer rank;
    private Integer playerId;
    private String playerName;
    private Long totalPoints;
    private boolean ineligible; // true if absences > 14
}