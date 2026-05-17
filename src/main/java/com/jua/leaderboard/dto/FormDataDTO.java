package com.jua.leaderboard.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormDataDTO {
    private Integer playerId;
    private String playerName;
    private List<FormMatchDTO> recentMatches;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormMatchDTO {
        private Integer matchNumber;
        private Integer position;   // null if absent
        private Integer points;     // null if absent
        private String result;      // "1st", "2nd", "3rd", "absent"
    }
}