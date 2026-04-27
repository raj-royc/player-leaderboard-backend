package com.jua.leaderboard.repository;

import com.jua.leaderboard.entity.WeeklyBonus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyBonusRepository extends JpaRepository<WeeklyBonus, Integer> {

    boolean existsByWeekNumber(Integer weekNumber);
}