package com.jua.leaderboard.repository;

import com.jua.leaderboard.entity.Match;
import com.jua.leaderboard.entity.MatchAbsence;
import com.jua.leaderboard.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchAbsenceRepository extends JpaRepository<MatchAbsence, Integer> {

    boolean existsByMatchAndPlayer(Match match, Player player);

    // Absence count per player, with ineligibility flag (> 14 absences)
    @Query("""
    SELECT p.id, p.name,
           (p.manualAbsenceCount + COUNT(ma.id)),
           CASE WHEN (p.manualAbsenceCount + COUNT(ma.id)) > 14 THEN true ELSE false END
    FROM Player p
    LEFT JOIN MatchAbsence ma ON ma.player = p
    GROUP BY p.id, p.name, p.manualAbsenceCount
    ORDER BY (p.manualAbsenceCount + COUNT(ma.id)) DESC
""")
    List<Object[]> getAbsenceCounts();
}
