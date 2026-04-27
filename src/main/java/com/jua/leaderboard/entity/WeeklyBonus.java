package com.jua.leaderboard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "weekly_bonus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyBonus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "week_number", nullable = false, unique = true)
    private Integer weekNumber;

    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded = 2;
}