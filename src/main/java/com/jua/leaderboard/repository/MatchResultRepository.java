package com.jua.leaderboard.repository;

import com.jua.leaderboard.entity.Match;
import com.jua.leaderboard.entity.MatchResult;
import com.jua.leaderboard.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Integer> {

    List<MatchResult> findByMatch(Match match);

    boolean existsByMatchAndPlayer(Match match, Player player);

    // Overall leaderboard — match points + weekly bonus, with tiebreaker ordering
    @Query("""
        SELECT p.id, p.name,
               COALESCE(SUM(mr.pointsAwarded), 0) + COALESCE(wb_total.bonusTotal, 0),
               COALESCE(SUM(CASE WHEN mr.position = 1 THEN 1 ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN mr.position = 2 THEN 1 ELSE 0 END), 0)
        FROM Player p
        LEFT JOIN MatchResult mr ON mr.player = p
        LEFT JOIN (
            SELECT wb.player.id AS pid, SUM(wb.pointsAwarded) AS bonusTotal
            FROM WeeklyBonus wb
            GROUP BY wb.player.id
        ) wb_total ON wb_total.pid = p.id
        GROUP BY p.id, p.name, wb_total.bonusTotal
        ORDER BY
            (COALESCE(SUM(mr.pointsAwarded), 0) + COALESCE(wb_total.bonusTotal, 0)) DESC,
            COALESCE(SUM(CASE WHEN mr.position = 1 THEN 1 ELSE 0 END), 0) DESC,
            COALESCE(SUM(CASE WHEN mr.position = 2 THEN 1 ELSE 0 END), 0) DESC
    """)
    List<Object[]> getOverallLeaderboard();

    // Weekly leaderboard — points for a specific week with tiebreaker ordering
    @Query("""
        SELECT p.id, p.name, COALESCE(SUM(mr.pointsAwarded), 0),
               COALESCE(SUM(CASE WHEN mr.position = 1 THEN 1 ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN mr.position = 2 THEN 1 ELSE 0 END), 0)
        FROM Player p
        LEFT JOIN MatchResult mr ON mr.player = p AND mr.match.weekNumber = :weekNumber
        GROUP BY p.id, p.name
        ORDER BY
            COALESCE(SUM(mr.pointsAwarded), 0) DESC,
            COALESCE(SUM(CASE WHEN mr.position = 1 THEN 1 ELSE 0 END), 0) DESC,
            COALESCE(SUM(CASE WHEN mr.position = 2 THEN 1 ELSE 0 END), 0) DESC
    """)
    List<Object[]> getWeeklyLeaderboard(@Param("weekNumber") Integer weekNumber);

    // Used for weekly bonus calculation — points within a specific week
    @Query("""
        SELECT mr.player.id, mr.player.name, SUM(mr.pointsAwarded),
               SUM(CASE WHEN mr.position = 1 THEN 1 ELSE 0 END),
               SUM(CASE WHEN mr.position = 2 THEN 1 ELSE 0 END)
        FROM MatchResult mr
        WHERE mr.match.weekNumber = :weekNumber
        GROUP BY mr.player.id, mr.player.name
        ORDER BY
            SUM(mr.pointsAwarded) DESC,
            SUM(CASE WHEN mr.position = 1 THEN 1 ELSE 0 END) DESC,
            SUM(CASE WHEN mr.position = 2 THEN 1 ELSE 0 END) DESC
    """)
    List<Object[]> getWeeklyPointsForBonus(@Param("weekNumber") Integer weekNumber);

    @Query("""
    SELECT mr.player.id, mr.player.name,
           SUM(CASE WHEN mr.position = 1 THEN 1 ELSE 0 END),
           SUM(CASE WHEN mr.position = 2 THEN 1 ELSE 0 END),
           SUM(CASE WHEN mr.position = 3 THEN 1 ELSE 0 END)
    FROM MatchResult mr
    GROUP BY mr.player.id, mr.player.name
    ORDER BY
        SUM(CASE WHEN mr.position = 1 THEN 1 ELSE 0 END) DESC,
        SUM(CASE WHEN mr.position = 2 THEN 1 ELSE 0 END) DESC,
        SUM(CASE WHEN mr.position = 3 THEN 1 ELSE 0 END) DESC
""")
    List<Object[]> getPodiumFinishes();

    @Query("""
    SELECT mr.player.id, COUNT(DISTINCT mr.match.id)
    FROM MatchResult mr
    WHERE mr.match.isCompleted = true
    GROUP BY mr.player.id
""")
    List<Object[]> getAttendedMatchCountPerPlayer();
}