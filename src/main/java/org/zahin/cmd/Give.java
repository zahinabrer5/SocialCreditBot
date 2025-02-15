package org.zahin.cmd;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.zahin.db.DatabaseHandler;
import org.zahin.util.CustomEmbed;

import java.math.BigInteger;

public class Give extends Cmd {
    private final DatabaseHandler dbHandler;

    public Give(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        // the "user" option is required, so it doesn't need a null-check here
        User user = event.getOption("user").getAsUser();
        long amount = event.getOption("amount").getAsLong();
        // however, we must do null-check on "reason" option since it's optional
        String reason = "";
        OptionMapping reasonOption = event.getOption("reason");
        if (reasonOption != null) {
            reason = reasonOption.getAsString();
        }
        give(event, user, BigInteger.valueOf(amount), reason);
    }

    private void give(SlashCommandInteractionEvent event, User user, BigInteger amount, String reason) {
        if (amount.compareTo(BigInteger.ZERO) <= 0) {
            event.reply("`amount` must be positive!").setEphemeral(true).queue();
            return;
        }

        if (!reason.isEmpty()) {
            reason = String.format("**__Reason:__** %s%n%n", reason);
        }

        String userId = user.getId();
        String eventUserId = event.getUser().getId();

        if (userId.equals(eventUserId)) {
            event.reply("You can't give yourself credits, you poopoo!!!").queue();
            return;
        }

        dbHandler.update(userId, amount);
        dbHandler.update(eventUserId, amount.negate());
        BigInteger balance = dbHandler.read(userId).balance();
        BigInteger eventUserBalance = dbHandler.read(eventUserId).balance();

        CustomEmbed embed = new CustomEmbed();
        embed.setTitle(String.format("%s just gave %s %d social credit!",
                event.getUser().getName(), user.getName(), amount));
        embed.setDescription(String.format("""
                        %s<@%s> now has **%d** social credit
                        <@%s> now has **%d** social credit""",
                reason, userId, balance, eventUserId, eventUserBalance));
        embed.setThumbnail("https://i.imgur.com/HsM6YU1.png");
        embed.setColor(0x2eb33e);

        event.replyEmbeds(embed.build()).queue();
    }
}
