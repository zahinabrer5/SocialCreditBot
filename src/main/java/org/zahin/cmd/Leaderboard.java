package org.zahin.cmd;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.zahin.db.DatabaseHandler;
import org.zahin.util.CustomEmbed;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;

public class Leaderboard extends Cmd {
    private final DatabaseHandler dbHandler;
    private final Dotenv dotenv;

    public Leaderboard(DatabaseHandler dbHandler, Dotenv dotenv) {
        this.dbHandler = dbHandler;
        this.dotenv = dotenv;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        OptionMapping maxOption = event.getOption("max");
        int max = 10;
        if (maxOption != null)
            max = maxOption.getAsInt();
        leaderboard(event, max);
    }

    private void leaderboard(SlashCommandInteractionEvent event, int max) {
        if (max < 1) {
            event.reply("`max` option must be at least 1!").setEphemeral(true).queue();
            return;
        }
        if (max > 20) {
            event.reply("`max` option can be at most 20!").setEphemeral(true).queue();
            return;
        }

        CustomEmbed embed = new CustomEmbed(dotenv);
        embed.setTitle("Top " + max + " Best Citizens of " + dotenv.get("MAIN_SERVER"));
        embed.setColor(0xfcdb00);

        Map<String, BigInteger> ranking = dbHandler.getRanking();
        int i = 1;
        Iterator<Map.Entry<String, BigInteger>> it = ranking.entrySet().iterator();
        while (it.hasNext() && i <= max) {
            Map.Entry<String, BigInteger> entry = it.next();
            embed.appendDescription(String.format("%d. <@%s>: **%d** social credit%n", i, entry.getKey(), entry.getValue()));
            i++;
        }

        event.replyEmbeds(embed.build()).queue();

        // ToDo: fix user IDs not showing profile on Discord ("You don't have access to this link")
    }
}
