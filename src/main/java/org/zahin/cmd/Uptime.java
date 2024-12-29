package org.zahin.cmd;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.Bot;
import org.zahin.util.Util;

public class Uptime extends Cmd {
    @Override
    public void run(SlashCommandInteractionEvent event) {
        event.reply("I've been awake for " + formatTime(System.nanoTime() - Bot.startTime)).queue();
    }

    /**
     * @param ns nanoseconds
     * @return a nice formatted String of days, hours, minutes and seconds
     */
    // https://stackoverflow.com/a/45075606/21405641
    private String formatTime(long ns) {
        long tempSec = ns / (1000 * 1000 * 1000);
        long sec = tempSec % 60;
        long min = (tempSec / 60) % 60;
        long hour = (tempSec / (60 * 60)) % 24;
        long day = (tempSec / (24 * 60 * 60)) % 24;

        String dayStr = Util.pluralizer("day", "days", day);
        String hourStr = Util.pluralizer("hour", "hours", hour);
        String minStr = Util.pluralizer("minute", "minutes", min);
        String secStr = Util.pluralizer("second", "seconds", sec);
        return String.format("%d %s, %d %s, %d %s, %d %s", day, dayStr, hour, hourStr, min, minStr, sec, secStr);
    }
}
