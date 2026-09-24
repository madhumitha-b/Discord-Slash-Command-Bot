package com.example.project;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

@Configuration
public class DiscordBotConfig {

    @Value("${discord.bot.token}")
    private String token;

@Bean
public JDA jda() {
    JDA jda = JDABuilder.createDefault(token)
            .addEventListeners(new SlashCommandListener())
            .build();

jda.updateCommands()
        .addCommands(
                Commands.slash("report", "Submit a report")
                        .addOption(OptionType.STRING, "text", "Report description", true),

                Commands.slash("status", "Check the bot status")
        )
        .queue();

    return  jda;
}
}