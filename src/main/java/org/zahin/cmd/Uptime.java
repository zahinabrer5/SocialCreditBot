package org.zahin.cmd;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.Bot;
import org.zahin.util.Util;

import java.time.Duration;
import java.time.Instant;

public class Uptime extends Cmd {
    @Override
    public void run(SlashCommandInteractionEvent event) {
        long nanos = Duration.between(Bot.startTime, Instant.now()).toNanos();
        event.reply("I've been awake for " + Util.formatTimeWithDays(nanos)).queue();
    }
}
