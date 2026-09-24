package com.example.project;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if (event.getName().equals("hello")) {
            event.reply("Hello! 👋").queue();
        }

        if (event.getName().equals("status")) {
            event.reply("Bot is running! ✅").queue();
        }

        if (event.getName().equals("report")) {

            String reportText = event.getOption("text").getAsString();

            event.reply("Report received: " + reportText).queue();
        }
    }
}