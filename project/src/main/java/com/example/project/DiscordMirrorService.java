/*package com.example.project;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DiscordMirrorService {

    private final JDA jda;
    private final String mirrorChannelId;

    public DiscordMirrorService(
            JDA jda,
            @Value("${discord.mirror.channel.id}") String mirrorChannelId) {

        this.jda = jda;
        this.mirrorChannelId = mirrorChannelId;
    }

    public void sendToMirrorChannel(String message) {

        TextChannel channel = jda.getTextChannelById(mirrorChannelId);

        if (channel == null) {
            System.out.println("Mirror channel not found: " + mirrorChannelId);
            return;
        }

        channel.sendMessage(message)
                .queue(
                        success -> System.out.println("Mirror message sent successfully"),
                        error -> System.out.println(
                                "Mirror failed: " + error.getMessage()
                        )
                );
    }
}*/