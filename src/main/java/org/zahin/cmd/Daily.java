package org.zahin.cmd;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.Bot;
import org.zahin.db.DatabaseHandler;
import org.zahin.util.Util;

import java.math.BigInteger;
import java.time.LocalDate;

public class Daily extends Cmd {
    private final DatabaseHandler dbHandler;

    public Daily(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        daily(event);
    }

    private void daily(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();

        if (Util.oneDayCooldown(event, dbHandler.getLastDailyUse(userId), Bot.tz, "daily")) {
            return;
        }

        BigInteger amount = Util.randomBigInteger(BigInteger.ZERO, BigInteger.valueOf(100), Bot.rand);
        dbHandler.update(userId, amount);

        dbHandler.setLastDailyUse(userId, LocalDate.now(Bot.tz));

        event.getHook().editOriginal(String.format("You just got **%d** social credit for today! Remember to do `/daily` tomorrow!", amount)).queue();
    }
}
