package com.jua.leaderboard.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "match_absences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchAbsence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;
}