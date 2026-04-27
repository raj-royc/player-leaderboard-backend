package com.jua.leaderboard.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PodiumDTO {

    private Integer playerId;
    private String playerName;
    private Long firstPlace;
    private Long secondPlace;
    private Long thirdPlace;
}
