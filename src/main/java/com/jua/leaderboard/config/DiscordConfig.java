package com.jua.leaderboard.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import com.jua.leaderboard.service.DiscordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordConfig {

    @Value("${DISCORD_BOT_TOKEN}")
    private String botToken;

    @Bean
    public JDA jda(DiscordService discordService) throws InterruptedException {
        JDA jda = JDABuilder.createDefault(botToken)
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .build();
        jda.addEventListener(discordService);
        jda.awaitReady();

        System.out.println("Discord bot connected as: " + jda.getSelfUser().getName());
        return jda;
    }
}