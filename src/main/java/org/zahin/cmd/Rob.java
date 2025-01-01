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

        BigInteger userCredits = dbHandler.read(user.getId()).balance();
        BigInteger min = userCredits.divide(BigInteger.TWO).negate();
        BigInteger amount = Util.randomBigInteger(min, userCredits, rand);

        dbHandler.update(event.getUser().getId(), amount);
        dbHandler.update(user.getId(), amount.negate());

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
        event.replyEmbeds(embed.build()).queue();
    }
}
