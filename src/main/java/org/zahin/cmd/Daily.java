package org.zahin.cmd;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.db.DatabaseHandler;
import org.zahin.util.Util;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;

public class Daily extends Cmd {
    private final DatabaseHandler dbHandler;
    private final Random rand;
    private final ZoneId z;

    public Daily(DatabaseHandler dbHandler, Random rand, ZoneId z) {
        this.dbHandler = dbHandler;
        this.rand = rand;
        this.z = z;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        daily(event);
    }

    private void daily(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        LocalDate today = LocalDate.now(z);
        if (!dbHandler.getLastDailyUse(userId).isBefore(today)) {
            ZonedDateTime now = ZonedDateTime.now(z);
            ZonedDateTime tomorrowMidnight = today.plusDays(1).atStartOfDay(z);
            long nanosTillTomorrow = Duration.between(now, tomorrowMidnight).toNanos();
            event.reply(String.format("You have to wait %s to use `/daily` again...", Util.formatTime(nanosTillTomorrow))).queue();
            return;
        }

        dbHandler.setLastDailyUse(userId, today);
        BigInteger amount = Util.randomBigInteger(BigInteger.ZERO, BigInteger.valueOf(100), rand);
        dbHandler.update(userId, amount);

        event.reply(String.format("You just got **%d** social credit for today! Remember to do `/daily` tomorrow!", amount)).queue();
    }
}
