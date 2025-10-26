package org.zahin.cmd;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.Bot;
import org.zahin.db.DatabaseHandler;
import org.zahin.util.Util;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Random;

public class Daily extends Cmd {
    private final DatabaseHandler dbHandler;
    private static final Random rand = new Random();

    public Daily(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        daily(event);
    }

    private void daily(SlashCommandInteractionEvent event) {
        event.reply("Checking cooldown...").queue();

        String userId = event.getUser().getId();

        long cooldown = Util.oneDayCooldown(dbHandler.getLastDailyUse(userId), Bot.tz);
        if (cooldown > 0) {
            event.getHook().editOriginal(String.format("You must wait %s to use `/daily` again!", Util.formatTime(cooldown))).queue();
            return;
        }

        BigInteger amount = Util.randomBigInteger(BigInteger.ZERO, BigInteger.valueOf(100), rand);
        dbHandler.update(userId, amount);

        dbHandler.setLastDailyUse(userId, LocalDate.now(Bot.tz));

        event.getHook().editOriginal(String.format("You just got **%d** social credit for today! Remember to do `/daily` tomorrow!", amount)).queue();
    }
}
