package com.jua.leaderboard.service;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordListener extends ListenerAdapter {

    private final DiscordService discordService;
    private final String channelId;

    public DiscordListener(DiscordService discordService, String channelId) {
        this.discordService = discordService;
        this.channelId = channelId;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        System.out.println("RAW MESSAGE: " + event.getMessage().getContentRaw()
                + " | channel: " + event.getChannel().getId());
        if (event.getAuthor().isBot()) return;
        if (!event.getChannel().getId().equals(channelId)) return;

        String content = event.getMessage().getContentRaw().trim().toLowerCase();

        switch (content) {
            case "!showme" -> {
                String msg = discordService.buildOverallMessage() + "\n"
                        + discordService.buildWeeklyMessage() + "\n"
                        + discordService.buildLastMatchMessage();
                event.getChannel().sendMessage(msg).queue();
            }
            case "!leaderboard" -> event.getChannel().sendMessage(discordService.buildOverallMessage()).queue();
            case "!weekly"      -> event.getChannel().sendMessage(discordService.buildWeeklyMessage()).queue();
            case "!podium"      -> event.getChannel().sendMessage(discordService.buildPodiumMessage()).queue();
            case "!missed"      -> event.getChannel().sendMessage(discordService.buildMissedMessage()).queue();
        }
    }
}