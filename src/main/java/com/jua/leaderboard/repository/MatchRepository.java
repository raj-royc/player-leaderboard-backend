package com.jua.leaderboard.repository;

import com.jua.leaderboard.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Integer> {

    Optional<Match> findByMatchNumber(Integer matchNumber);

    // Count how many week-ending matches exist before this match number
    // This gives us the week number (result + 1)
    @Query("SELECT COUNT(m) FROM Match m WHERE m.isWeekEnd = true AND m.matchNumber < :matchNumber")
    Integer countWeekEndsBeforeMatch(Integer matchNumber);
}