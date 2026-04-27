package com.jua.leaderboard.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchSubmitRequestDTO {

    private Integer matchNumber;
    private LocalDate matchDate;

    private Integer firstPlacePlayerId;
    private Integer secondPlacePlayerId;
    private Integer thirdPlacePlayerId;

    private Integer bonusPoints;

    private List<Integer> absentPlayerIds;

    private Boolean isLastGameOfWeek = false;
}