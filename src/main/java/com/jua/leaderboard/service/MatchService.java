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
//@RequiredArgsConstructor
public class MatchService {

    private final DiscordService discordService;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchAbsenceRepository matchAbsenceRepository;
    private final WeeklyBonusRepository weeklyBonusRepository;

    public MatchService(MatchRepository matchRepository,
                        PlayerRepository playerRepository,
                        MatchResultRepository matchResultRepository,
                        MatchAbsenceRepository matchAbsenceRepository,
                        WeeklyBonusRepository weeklyBonusRepository,
                        @Lazy DiscordService discordService) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.matchResultRepository = matchResultRepository;
        this.matchAbsenceRepository = matchAbsenceRepository;
        this.weeklyBonusRepository = weeklyBonusRepository;
        this.discordService = discordService;
    }

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
        if (weeklyBonusRepository.existsByWeekNumber(weekNumber)) return;

        List<Object[]> rows = matchResultRepository.getWeeklyPointsForBonus(weekNumber);
        if (rows.isEmpty()) return;

        // Find first eligible player
        for (Object[] row : rows) {
            Integer winnerId = (Integer) row[0];
            Long absences = matchAbsenceRepository.getAbsenceCounts()
                    .stream()
                    .filter(r -> r[0].equals(winnerId))
                    .map(r -> (Long) r[2])
                    .findFirst()
                    .orElse(0L);

            if (absences <= 14) {
                Player winnerPlayer = playerRepository.findById(winnerId)
                        .orElseThrow(() -> new RuntimeException("Winner player not found"));
                weeklyBonusRepository.save(new WeeklyBonus(null, winnerPlayer, weekNumber, 2));
                return;
            }
        }
    }

    public List<OverallLeaderboardDTO> getOverallLeaderboard() {
        List<Object[]> rows = matchResultRepository.getOverallLeaderboard();

        // Build absence map — player_id -> total absences (manual + logged)
        Map<Integer, Long> absenceMap = matchAbsenceRepository.getAbsenceCounts()
                .stream()
                .collect(Collectors.toMap(
                        r -> (Integer) r[0],
                        r -> (Long) r[2]
                ));

        // Build attended matches map — player_id -> matches attended
        Map<Integer, Long> attendedMap = matchResultRepository.getAttendedMatchCountPerPlayer()
                .stream()
                .collect(Collectors.toMap(
                        r -> (Integer) r[0],
                        r -> (Long) r[1]
                ));

        List<OverallLeaderboardDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            Integer playerId = (Integer) row[0];
            String playerName = (String) row[1];
            double rawPoints = ((Number) row[2]).doubleValue();

            Long absences = absenceMap.getOrDefault(playerId, 0L);
            boolean ineligible = absences > 14;

            long attended = attendedMap.getOrDefault(playerId, 0L);
            boolean isNormalised = attended > 60;

            double finalPoints;
            if (isNormalised) {
                // formula: (total points / matches attended) * 60, to 3 decimal places
                finalPoints = Math.round((rawPoints / attended) * 59 * 1000.0) / 1000.0;
            } else {
                finalPoints = rawPoints;
            }

            // ineligible players get 0 points for ranking purposes
            double rankingPoints = ineligible ? 0.0 : finalPoints;

            result.add(new OverallLeaderboardDTO(
                    0, // rank assigned after sorting
                    playerId,
                    playerName,
                    rankingPoints,
                    ineligible,
                    (int) attended,
                    isNormalised
            ));
        }

        // Sort — ineligible always go to bottom, then by points desc
        result.sort((a, b) -> {
            if (a.isIneligible() && !b.isIneligible()) return 1;
            if (!a.isIneligible() && b.isIneligible()) return -1;
            return Double.compare(b.getTotalPoints(), a.getTotalPoints());
        });

        // Assign ranks after sorting
        int rank = 1;
        for (OverallLeaderboardDTO entry : result) {
            entry.setRank(rank++);
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

    //changes for Analytics tab

    public List<CumulativeDataDTO> getCumulativeData() {
        List<Object[]> rows = matchResultRepository.getCumulativePointsData();

        // Group by player
        Map<Integer, CumulativeDataDTO> playerMap = new java.util.LinkedHashMap<>();

        for (Object[] row : rows) {
            Integer playerId = (Integer) row[0];
            String playerName = (String) row[1];
            Integer matchNumber = (Integer) row[2];
            Integer points = ((Number) row[3]).intValue();

            playerMap.putIfAbsent(playerId, new CumulativeDataDTO(playerId, playerName, new ArrayList<>()));
            CumulativeDataDTO dto = playerMap.get(playerId);

            int cumulative = dto.getMatches().isEmpty() ? points
                    : dto.getMatches().get(dto.getMatches().size() - 1).getCumulative() + points;

            dto.getMatches().add(new CumulativeDataDTO.MatchPointDTO(matchNumber, points, cumulative));
        }

        return new ArrayList<>(playerMap.values());
    }

    public FormDataDTO getPlayerForm(Integer playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        List<Object[]> rows = matchResultRepository.getPlayerMatchHistory(playerId);

        // Take last 5 only
        List<FormDataDTO.FormMatchDTO> recentMatches = rows.stream()
                .limit(5)
                .map(row -> {
                    Integer matchNumber = (Integer) row[0];
                    Integer position = row[1] != null ? ((Number) row[1]).intValue() : null;
                    Integer points = row[2] != null ? ((Number) row[2]).intValue() : null;
                    String result = position == null ? "absent"
                            : position == 1 ? "1st"
                            : position == 2 ? "2nd" : "3rd";
                    return new FormDataDTO.FormMatchDTO(matchNumber, position, points, result);
                })
                .collect(Collectors.toList());

        return new FormDataDTO(playerId, player.getName(), recentMatches);
    }

    public List<PodiumRateDTO> getPodiumRateData() {
        List<Object[]> rows = matchResultRepository.getPodiumRateData();
        List<PodiumRateDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            Integer playerId = (Integer) row[0];
            String playerName = (String) row[1];
            Long attended = (Long) row[2];
            Long podiums = (Long) row[3];
            double rate = attended > 0 ? Math.round((podiums * 100.0 / attended) * 10) / 10.0 : 0.0;
            result.add(new PodiumRateDTO(playerId, playerName, attended, podiums, rate));
        }
        result.sort((a, b) -> Double.compare(b.getPodiumRate(), a.getPodiumRate()));
        return result;
    }

}