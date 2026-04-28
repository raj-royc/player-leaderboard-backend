package com.jua.leaderboard.service;

import com.jua.leaderboard.dto.*;
import com.jua.leaderboard.entity.*;
import com.jua.leaderboard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    @Lazy
    private final DiscordService discordService;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchAbsenceRepository matchAbsenceRepository;
    private final WeeklyBonusRepository weeklyBonusRepository;

    @Transactional
    public void submitMatchResult(MatchSubmitRequestDTO request) {

        // 1. Find the match
        Match match = matchRepository.findByMatchNumber(request.getMatchNumber())
                .orElseThrow(() -> new RuntimeException("Match not found: " + request.getMatchNumber()));

        // 2. Check not already completed
        if (match.getIsCompleted()) {
            throw new RuntimeException("Match " + request.getMatchNumber() + " is already completed");
        }

        // 3. Validate bonus points
        Integer bonus = request.getBonusPoints();
        if (bonus != null && bonus != 1 && bonus != 2) {
            throw new RuntimeException("Bonus points must be 1 or 2 only");
        }

        // 4. Fetch the 3 players
        Player first = playerRepository.findById(request.getFirstPlacePlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));
        Player second = playerRepository.findById(request.getSecondPlacePlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));
        Player third = playerRepository.findById(request.getThirdPlacePlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        // 5. Calculate week number from is_week_end markers
        Integer weekNumber = matchRepository.countWeekEndsBeforeMatch(request.getMatchNumber()) + 1;

        // 6. Calculate points
        int firstPoints = 4 + (bonus != null ? bonus : 0);

        // 7. Save match results
        matchResultRepository.save(new MatchResult(null, match, first, 1, firstPoints));
        matchResultRepository.save(new MatchResult(null, match, second, 2, 2));
        matchResultRepository.save(new MatchResult(null, match, third, 3, 1));

        // 8. Save absences
        if (request.getAbsentPlayerIds() != null) {
            for (Integer playerId : request.getAbsentPlayerIds()) {
                Player absentPlayer = playerRepository.findById(playerId)
                        .orElseThrow(() -> new RuntimeException("Absent player not found: " + playerId));
                matchAbsenceRepository.save(new MatchAbsence(null, match, absentPlayer));
            }
        }

        // 9. Update match record
        match.setMatchDate(request.getMatchDate());
        match.setWeekNumber(weekNumber);
        match.setIsCompleted(true);
        match.setIsWeekEnd(Boolean.TRUE.equals(request.getIsLastGameOfWeek()));
        matchRepository.save(match);

        // 10. If last game of week, award weekly bonus
        if (Boolean.TRUE.equals(request.getIsLastGameOfWeek())) {
            awardWeeklyBonus(weekNumber);
        }
        String weeklyBonusWinnerName = null;
        if (Boolean.TRUE.equals(request.getIsLastGameOfWeek())) {
            weeklyBonusWinnerName = weeklyBonusRepository.findByWeekNumber(weekNumber)
                    .map(wb -> wb.getPlayer().getName())
                    .orElse(null);
        }
        discordService.postMatchUpdate(
                request.getMatchNumber(),
                first.getName(),
                second.getName(),
                third.getName(),
                Boolean.TRUE.equals(request.getIsLastGameOfWeek()),
                weeklyBonusWinnerName
        );
    }

    private void awardWeeklyBonus(Integer weekNumber) {
        // Guard — don't double award if somehow called twice
        if (weeklyBonusRepository.existsByWeekNumber(weekNumber)) return;

        List<Object[]> rows = matchResultRepository.getWeeklyPointsForBonus(weekNumber);
        if (rows.isEmpty()) return;

        // First row is the winner after tiebreaker ordering in the query
        Object[] winner = rows.get(0);
        Integer winnerId = (Integer) winner[0];
        Player winnerPlayer = playerRepository.findById(winnerId)
                .orElseThrow(() -> new RuntimeException("Winner player not found"));

        weeklyBonusRepository.save(new WeeklyBonus(null, winnerPlayer, weekNumber, 2));
    }

    public List<OverallLeaderboardDTO> getOverallLeaderboard() {
        List<Object[]> rows = matchResultRepository.getOverallLeaderboard();
        Map<Integer, Long> absenceMap = matchAbsenceRepository.getAbsenceCounts()
                .stream()
                .collect(Collectors.toMap(
                        r -> (Integer) r[0],
                        r -> (Long) r[2]
                ));

        List<OverallLeaderboardDTO> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            Integer playerId = (Integer) row[0];
            String playerName = (String) row[1];
            Long totalPoints = ((Number) row[2]).longValue();
            Long absences = absenceMap.getOrDefault(playerId, 0L);
            boolean ineligible = absences > 14;
            result.add(new OverallLeaderboardDTO(rank++, playerId, playerName, totalPoints, ineligible));
        }
        return result;
    }

    public int getCurrentWeekNumber() {
        return matchRepository.findAll()
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsCompleted()) && m.getWeekNumber() != null)
                .mapToInt(m -> m.getWeekNumber())
                .max()
                .orElse(1);
    }
    public List<WeeklyLeaderboardDTO> getWeeklyLeaderboard(Integer weekNumber) {
        List<Object[]> rows = matchResultRepository.getWeeklyLeaderboard(weekNumber);
        List<WeeklyLeaderboardDTO> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            result.add(new WeeklyLeaderboardDTO(
                    rank++,
                    (Integer) row[0],
                    (String) row[1],
                    ((Number) row[2]).longValue()
            ));
        }
        return result;
    }

    public List<AbsenceDTO> getAbsences() {
        List<Object[]> rows = matchAbsenceRepository.getAbsenceCounts();
        List<AbsenceDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new AbsenceDTO(
                    (Integer) row[0],
                    (String) row[1],
                    (Long) row[2],
                    (boolean) row[3]
            ));
        }
        return result;
    }

    public List<PodiumDTO> getPodiumFinishes() {
        List<Object[]> rows = matchResultRepository.getPodiumFinishes();
        List<PodiumDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new PodiumDTO(
                    (Integer) row[0],
                    (String) row[1],
                    (Long) row[2],
                    (Long) row[3],
                    (Long) row[4]
            ));
        }
        return result;
    }

    public int getLastCompletedMatchNumber() {
        return matchRepository.findAll()
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsCompleted()))
                .mapToInt(Match::getMatchNumber)
                .max()
                .orElseThrow(() -> new RuntimeException("No completed matches"));
    }

    public MatchTopThreeResult getMatchTopThree(int matchNumber) {
        Match match = matchRepository.findByMatchNumber(matchNumber)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        List<MatchResult> results = matchResultRepository.findByMatch(match);
        String first = results.stream().filter(r -> r.getPosition() == 1).findFirst().map(r -> r.getPlayer().getName()).orElse("?");
        String second = results.stream().filter(r -> r.getPosition() == 2).findFirst().map(r -> r.getPlayer().getName()).orElse("?");
        String third = results.stream().filter(r -> r.getPosition() == 3).findFirst().map(r -> r.getPlayer().getName()).orElse("?");
        return new MatchTopThreeResult(first, second, third);
    }

    // Simple inner record to hold the result
    public record MatchTopThreeResult(String first, String second, String third) {}
}