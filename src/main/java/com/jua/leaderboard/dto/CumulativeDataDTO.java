package com.jua.leaderboard.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CumulativeDataDTO {
    private Integer playerId;
    private String playerName;
    private List<MatchPointDTO> matches;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchPointDTO {
        private Integer matchNumber;
        private Integer points;
        private Integer cumulative;
    }
}