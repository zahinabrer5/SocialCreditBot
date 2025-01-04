package org.zahin.cmd;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.db.DatabaseHandler;
import org.zahin.util.CustomEmbed;
import org.zahin.util.Util;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Random;

public class Rob extends Cmd {
    private final DatabaseHandler dbHandler;
    private final Dotenv dotenv;
    private final Random rand;
    private final ZoneId z;

    public Rob(DatabaseHandler dbHandler, Dotenv dotenv, Random rand, ZoneId z) {
        this.dbHandler = dbHandler;
        this.dotenv = dotenv;
        this.rand = rand;
        this.z = z;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        User user = event.getOption("user").getAsUser();
        rob(event, user);
    }

    private void rob(SlashCommandInteractionEvent event, User user) {
        String robberId = event.getUser().getId();
        String victimId = user.getId();

        if (Util.oneDayCooldown(event, dbHandler.getLastRobUse(robberId), z, "rob"))
            return;

        if (robberId.equals(victimId)) {
            event.reply("You can't rob from yourself, silly!").queue();
            return;
        }

        if (victimId.equals(dotenv.get("OWNER_ID")) || victimId.equals(dotenv.get("DEV_ID"))) {
            event.reply("You can't rob from these guys... NOOB").queue();
            return;
        }

        // minimum possible rob will be -50% of the victim's wealth
        BigInteger victimBalance = dbHandler.read(victimId).balance();
        BigInteger min = victimBalance.divide(BigInteger.TWO).negate();

        // maximum possible rob starts off at 100% of victim's balance but decreases exponentially as
        // robber begins to rob more (so robber is exponentially less likely to make a profit the more they rob)
        int numRobs = dbHandler.getNumRobs(robberId);
        BigInteger max = Util.scaleBigInteger(victimBalance, Math.pow(0.6, numRobs));

        // if the absolute difference between the balances of the robber and victim is at least 1000,
        // then the robber can rob from at most 10% of the victim's wealth
        BigInteger robberBalance = dbHandler.read(robberId).balance();
        BigInteger balanceDiff = robberBalance.subtract(victimBalance).abs();
        BigInteger maxScaled = Util.scaleBigInteger(victimBalance, 0.1);
        if (balanceDiff.compareTo(BigInteger.valueOf(1000)) >= 0 && maxScaled.compareTo(max) < 0)
            max = maxScaled;

        // set a hard maximum possible rob of 500 social credit
        BigInteger fiveHundred = BigInteger.valueOf(500);
        if (max.compareTo(fiveHundred) > 0)
            max = fiveHundred;

        // all of the above constraints are set to make it harder for people to exploit /rob and
        // get rich very quickly...
        BigInteger amount = Util.randomBigInteger(min, max, rand);

        dbHandler.update(robberId, amount);
        dbHandler.update(victimId, amount.negate());

        dbHandler.setLastRobUse(robberId, LocalDate.now(z));

        CustomEmbed embed = new CustomEmbed(dotenv);
        embed.setTitle(String.format("%s just attempted to rob %s...", event.getUser().getName(), user.getName()));
        if (amount.compareTo(BigInteger.ZERO) < 0) {
            embed.setDescription(String.format("Instead, <@%s> got caught by <@%s> and had to pay them **%d** social credit!", robberId, victimId, amount.abs()));
            embed.setColor(0xff0000);
            embed.setThumbnail("https://i.imgur.com/l4sQ8lV.png");
        } else {
            embed.setDescription(String.format("<@%s> successfully robbed **%d** social credit from <@%s>!", robberId, amount.abs(), victimId));
            embed.setColor(0x2eb33e);
            embed.setThumbnail("https://i.imgur.com/HsM6YU1.png");
        }
        embed.appendDescription(String.format("<@%s> now has %d social credit%n", robberId, dbHandler.read(robberId).balance()));
        embed.appendDescription(String.format("<@%s> now has %d social credit", victimId, dbHandler.read(victimId).balance()));
        event.replyEmbeds(embed.build()).queue();
    }
}
