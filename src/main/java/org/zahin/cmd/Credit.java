package org.zahin.cmd;

import org.zahin.db.DatabaseHandler;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.zahin.util.Util;

import java.math.BigInteger;
import java.time.Instant;

public class Credit extends Cmd {
    private final DatabaseHandler dbHandler;
    private final Dotenv dotenv;

    public Credit(DatabaseHandler dbHandler, Dotenv dotenv) {
        this.dbHandler = dbHandler;
        this.dotenv = dotenv;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        // the "user" option is required, so it doesn't need a null-check here
        User user = event.getOption("user").getAsUser();
        long amount = event.getOption("amount").getAsLong();
        // however, we must do null-check on "reason" option since it's optional
        String reason = "";
        OptionMapping reasonOption = event.getOption("reason");
        if (reasonOption != null)
            reason = reasonOption.getAsString();
        credit(event, user, amount, reason);
    }

    private void credit(SlashCommandInteractionEvent event, User user, long amount, String reason) {
        if (amount == 0) {
            event.reply("`amount` cannot be 0 (zero)!").setEphemeral(true).queue();
            return;
        }

        if (!reason.isEmpty())
            reason = String.format("**__Reason:__** %s%n%n", reason);

        String userId = user.getId();
        dbHandler.update(userId, amount);
        BigInteger balance = dbHandler.read(userId).balance();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(dotenv.get("BOT_NAME"));
        eb.setTitle(Util.fmt.format(amount)+" social credit!");
        eb.setDescription(String.format("%s<@%s> now has **%d** social credit", reason, userId, balance));
//        eb.setFooter("Try /leaderboard");
        eb.setTimestamp(Instant.now());

        String img = "https://i.imgur.com/HsM6YU1.png";
        int colour = 0x2eb33e;
        if (amount < 0) {
            img = "https://i.imgur.com/l4sQ8lV.png";
            colour = 0xff0000;
        }
        eb.setThumbnail(img);
        eb.setColor(colour);

        event.replyEmbeds(eb.build()).queue();
    }
}
