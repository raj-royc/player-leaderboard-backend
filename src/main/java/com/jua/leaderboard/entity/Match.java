package com.jua.leaderboard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "match_number", nullable = false, unique = true)
    private Integer matchNumber;

    @Column(name = "match_date")
    private LocalDate matchDate;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "is_week_end", nullable = false)
    private Boolean isWeekEnd = false;
}