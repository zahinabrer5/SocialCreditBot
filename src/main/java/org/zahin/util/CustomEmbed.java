package org.zahin.util;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Instant;

public class CustomEmbed extends EmbedBuilder {
    public CustomEmbed(Dotenv dotenv) {
        this.setAuthor(dotenv.get("BOT_NAME"));
        this.setTimestamp(Instant.now());
    }
}
