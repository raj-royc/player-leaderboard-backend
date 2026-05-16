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
    private Double totalPoints;
    private boolean ineligible;
    private Integer matchesAttended;
    private boolean isNormalised;
}