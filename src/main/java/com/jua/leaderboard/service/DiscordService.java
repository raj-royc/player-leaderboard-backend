package com.jua.leaderboard.service;

import com.jua.leaderboard.dto.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscordService extends ListenerAdapter {

    @Value("${DISCORD_CHANNEL_ID}")
    private String channelId;

    private final MatchService matchService;
    private JDA jda;

    public DiscordService(MatchService matchService) {
        this.matchService = matchService;
    }

    @Autowired
    public void setJda(@Lazy JDA jda) {
        this.jda = jda;
    }

    // ─── Auto post after match submission ───────────────────────────────────

    public void postMatchUpdate(int matchNumber, String first, String second, String third,
                                boolean isWeekEnd, String weeklyBonusWinner) {
        if (jda == null) return;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🎮  MATCH ").append(matchNumber).append(" RESULT\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🥇  ").append(first).append("\n");
        sb.append("🥈  ").append(second).append("\n");
        sb.append("🥉  ").append(third).append("\n");
        if (isWeekEnd && weeklyBonusWinner != null) {
            sb.append("\n🌟  WEEKLY BONUS → ").append(weeklyBonusWinner).append(" +2 pts\n");
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("```\n");
        sb.append(buildOverallMessage());
        sb.append("\n");
        sb.append(buildWeeklyMessage());

        channel.sendMessage(sb.toString()).queue();
    }

    // ─── Discord command listener ────────────────────────────────────────────

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.getChannel().getId().equals(channelId)) return;

        String content = event.getMessage().getContentRaw().trim().toLowerCase();

        switch (content) {
            case "!showme" -> {
                String msg = buildOverallMessage() + "\n"
                        + buildWeeklyMessage() + "\n"
                        + buildLastMatchMessage();
                event.getChannel().sendMessage(msg).queue();
            }
            case "!leaderboard" -> event.getChannel().sendMessage(buildOverallMessage()).queue();
            case "!weekly"      -> event.getChannel().sendMessage(buildWeeklyMessage()).queue();
            case "!podium"      -> event.getChannel().sendMessage(buildPodiumMessage()).queue();
            case "!missed"      -> event.getChannel().sendMessage(buildMissedMessage()).queue();
        }
    }

    // ─── Message builders ────────────────────────────────────────────────────

    private String buildOverallMessage() {
        List<OverallLeaderboardDTO> overall = matchService.getOverallLeaderboard();
        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        sb.append("🏆  OVERALL LEADERBOARD\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        for (OverallLeaderboardDTO entry : overall) {
            String flag = entry.isIneligible() ? "  ⚠️ ineligible" : "";
            sb.append(String.format("%-3s %-15s %s pts%s\n",
                    entry.getRank() + ".",
                    entry.getPlayerName(),
                    entry.getTotalPoints(),
                    flag));
        }
        sb.append("```");
        return sb.toString();
    }

    private String buildWeeklyMessage() {
        int currentWeek = matchService.getCurrentWeekNumber();
        List<WeeklyLeaderboardDTO> weekly = matchService.getWeeklyLeaderboard(currentWeek);
        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        sb.append("📅  WEEK ").append(currentWeek).append(" STANDINGS\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        if (weekly.isEmpty()) {
            sb.append("No matches played this week yet.\n");
        } else {
            for (WeeklyLeaderboardDTO entry : weekly) {
                sb.append(String.format("%-3s %-15s %s pts\n",
                        entry.getRank() + ".",
                        entry.getPlayerName(),
                        entry.getTotalPoints()));
            }
        }
        sb.append("```");
        return sb.toString();
    }

    private String buildLastMatchMessage() {
        try {
            int lastMatch = matchService.getLastCompletedMatchNumber();
            var topThree = matchService.getMatchTopThree(lastMatch);
            StringBuilder sb = new StringBuilder();
            sb.append("```\n");
            sb.append("🎮  LAST MATCH — MATCH ").append(lastMatch).append("\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            sb.append("🥇  ").append(topThree.first()).append("\n");
            sb.append("🥈  ").append(topThree.second()).append("\n");
            sb.append("🥉  ").append(topThree.third()).append("\n");
            sb.append("```");
            return sb.toString();
        } catch (Exception e) {
            return "```\nNo completed matches yet.\n```";
        }
    }

    private String buildPodiumMessage() {
        List<PodiumDTO> podiums = matchService.getPodiumFinishes();
        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        sb.append("🎖️  PODIUM FINISHES\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append(String.format("%-15s  🥇 / 🥈 / 🥉\n", "Player"));
        sb.append("──────────────────────────────\n");
        if (podiums.isEmpty()) {
            sb.append("No data yet.\n");
        } else {
            for (PodiumDTO entry : podiums) {
                sb.append(String.format("%-15s  %s/%s/%s\n",
                        entry.getPlayerName(),
                        entry.getFirstPlace(),
                        entry.getSecondPlace(),
                        entry.getThirdPlace()));
            }
        }
        sb.append("```");
        return sb.toString();
    }

    private String buildMissedMessage() {
        List<AbsenceDTO> absences = matchService.getAbsences();
        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        sb.append("🚫  MATCHES MISSED\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        if (absences.isEmpty()) {
            sb.append("No absences recorded yet.\n");
        } else {
            for (AbsenceDTO entry : absences) {
                String flag = entry.isIneligible() ? "  ⚠️ OUT" : "";
                sb.append(String.format("%-15s  %s missed%s\n",
                        entry.getPlayerName(),
                        entry.getAbsenceCount(),
                        flag));
            }
        }
        sb.append("```");
        return sb.toString();
    }
}