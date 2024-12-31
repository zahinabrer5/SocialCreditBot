package org.zahin.cmd;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.Bot;
import org.zahin.util.Util;

public class Uptime extends Cmd {
    @Override
    public void run(SlashCommandInteractionEvent event) {
        event.reply("I've been awake for " + Util.formatTimeWithDays(System.nanoTime() - Bot.startTime)).queue();
    }
}
