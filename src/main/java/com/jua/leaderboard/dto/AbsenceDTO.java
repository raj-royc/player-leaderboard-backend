package com.jua.leaderboard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AbsenceDTO {

    private Integer playerId;
    private String playerName;
    private Long absenceCount;
    private boolean ineligible;
}