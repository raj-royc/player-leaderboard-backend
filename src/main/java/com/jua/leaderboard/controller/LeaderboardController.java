package com.jua.leaderboard.controller;

import com.jua.leaderboard.dto.*;
import com.jua.leaderboard.entity.Match;
import com.jua.leaderboard.entity.MatchResult;
import com.jua.leaderboard.entity.Player;
import com.jua.leaderboard.repository.MatchRepository;
import com.jua.leaderboard.repository.MatchResultRepository;
import com.jua.leaderboard.repository.PlayerRepository;
import com.jua.leaderboard.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173","https://player-leaderboard-frontend1.vercel.app"})
public class LeaderboardController {

    private final MatchService matchService;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;

    // Submit a match result
    @PostMapping("/matches/submit")
    public ResponseEntity<String> submitMatch(@RequestBody MatchSubmitRequestDTO request) {
        matchService.submitMatchResult(request);
        return ResponseEntity.ok("Match " + request.getMatchNumber() + " submitted successfully");
    }

    // Get all players (for populating dropdowns on the frontend)
    @GetMapping("/players")
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(playerRepository.findAll());
    }

    // Overall leaderboard
    @GetMapping("/leaderboard/overall")
    public ResponseEntity<List<OverallLeaderboardDTO>> getOverallLeaderboard() {
        return ResponseEntity.ok(matchService.getOverallLeaderboard());
    }

    // Weekly leaderboard
    @GetMapping("/leaderboard/weekly")
    public ResponseEntity<List<WeeklyLeaderboardDTO>> getWeeklyLeaderboard(
            @RequestParam Integer week) {
        return ResponseEntity.ok(matchService.getWeeklyLeaderboard(week));
    }

    // Absence tracker
    @GetMapping("/absences")
    public ResponseEntity<List<AbsenceDTO>> getAbsences() {
        return ResponseEntity.ok(matchService.getAbsences());
    }

    // Podium finishes
    @GetMapping("/podiums")
    public ResponseEntity<List<PodiumDTO>> getPodiumFinishes() {
        return ResponseEntity.ok(matchService.getPodiumFinishes());
    }

    @GetMapping("/matches")
    public ResponseEntity<?> getAllMatches() {
        return ResponseEntity.ok(matchRepository.findAll());
    }

    @GetMapping("/matches/{matchNumber}/topthree")
    public ResponseEntity<?> getMatchTopThree(@PathVariable Integer matchNumber) {
        Match match = matchRepository.findByMatchNumber(matchNumber)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        if (!match.getIsCompleted()) {
            return ResponseEntity.badRequest().body("Match not completed yet");
        }

        List<MatchResult> results = matchResultRepository.findByMatch(match);
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("matchNumber", match.getMatchNumber());
        response.put("matchDate", match.getMatchDate());
        results.stream().filter(r -> r.getPosition() == 1).findFirst()
                .ifPresent(r -> response.put("firstPlace", r.getPlayer().getName()));
        results.stream().filter(r -> r.getPosition() == 2).findFirst()
                .ifPresent(r -> response.put("secondPlace", r.getPlayer().getName()));
        results.stream().filter(r -> r.getPosition() == 3).findFirst()
                .ifPresent(r -> response.put("thirdPlace", r.getPlayer().getName()));
        return ResponseEntity.ok(response);
    }

    //changes for analytics tab
    @GetMapping("/analytics/cumulative")
    public ResponseEntity<List<CumulativeDataDTO>> getCumulativeData() {
        return ResponseEntity.ok(matchService.getCumulativeData());
    }

    @GetMapping("/analytics/form/{playerId}")
    public ResponseEntity<FormDataDTO> getPlayerForm(@PathVariable Integer playerId) {
        return ResponseEntity.ok(matchService.getPlayerForm(playerId));
    }

    @GetMapping("/analytics/podium-rate")
    public ResponseEntity<List<PodiumRateDTO>> getPodiumRate() {
        return ResponseEntity.ok(matchService.getPodiumRateData());
    }

}