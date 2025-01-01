package org.zahin.cmd;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.db.DatabaseHandler;
import org.zahin.util.Util;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
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

        if (Util.oneDayCooldown(event, dbHandler.getLastDailyUse(userId), z, "daily"))
            return;

        BigInteger amount = Util.randomBigInteger(BigInteger.ZERO, BigInteger.valueOf(100), rand);
        dbHandler.update(userId, amount);

        dbHandler.setLastDailyUse(userId, LocalDate.now(z));

        event.reply(String.format("You just got **%d** social credit for today! Remember to do `/daily` tomorrow!", amount)).queue();
    }
}
