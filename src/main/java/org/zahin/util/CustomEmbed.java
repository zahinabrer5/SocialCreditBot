package org.zahin.util;

import net.dv8tion.jda.api.EmbedBuilder;
import org.zahin.Bot;

import java.time.Instant;

public class CustomEmbed extends EmbedBuilder {
    public CustomEmbed() {
        this.setAuthor(Bot.dotenv.get("BOT_NAME"));
        this.setTimestamp(Instant.now());
    }
}
