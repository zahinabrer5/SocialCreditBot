package org.zahin.cmd;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.db.DatabaseHandler;
import org.zahin.util.Util;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Random;

public class Daily extends Cmd {
    private final DatabaseHandler dbHandler;
    private final Dotenv dotenv;
    private final Random rand;

    public Daily(DatabaseHandler dbHandler, Dotenv dotenv, Random rand) {
        this.dbHandler = dbHandler;
        this.dotenv = dotenv;
        this.rand = rand;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        daily(event);
    }

    private void daily(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        if (!dbHandler.getLastDailyUse(userId).isBefore(LocalDate.now())) {
            event.reply("You've already done `/daily` today... Try again tomorrow").queue();
            return;
        }

        dbHandler.setLastDailyUse(userId, LocalDate.now());
        BigInteger amount = Util.randomBigInteger(BigInteger.ZERO, BigInteger.valueOf(100), rand);
        dbHandler.update(userId, amount);

        event.reply(String.format("You just got **%d** social credit for today! Remember to do `/daily` tomorrow!", amount)).queue();
    }
}
