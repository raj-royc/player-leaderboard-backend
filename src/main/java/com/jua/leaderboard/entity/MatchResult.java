package com.jua.leaderboard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "match_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded;
}