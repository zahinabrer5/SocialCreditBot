package org.zahin.util;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.Instant;

public class CustomEmbed {
    private final EmbedBuilder eb;

    public CustomEmbed(Dotenv dotenv) {
        this.eb = new EmbedBuilder();
        eb.setAuthor(dotenv.get("BOT_NAME"));
        eb.setTimestamp(Instant.now());
    }

    public CustomEmbed setTitle(String title) {
        eb.setTitle(title);
        return this;
    }

    public CustomEmbed setDescription(String description) {
        eb.setDescription(description);
        return this;
    }

    public CustomEmbed setFooter(String footer) {
        eb.setFooter(footer);
        return this;
    }

    public CustomEmbed setColour(int colour) {
        eb.setColor(colour);
        return null;
    }

    public CustomEmbed setImage(String url) {
        eb.setImage(url);
        return this;
    }

    public MessageEmbed build() {
        return eb.build();
    }
}
