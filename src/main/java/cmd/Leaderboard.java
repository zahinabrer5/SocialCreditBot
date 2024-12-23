package cmd;

import db.DatabaseHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;

public class Leaderboard extends Cmd {
    private final DatabaseHandler dbHandler;

    public Leaderboard(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
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

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor("People's Republic of OC STEM");
        eb.setTitle("Top "+max+" Best Citizens of OC STEM");
        eb.setFooter("Try /profile");
        eb.setColor(new Color(0xfcdb00));

        StringBuilder description = new StringBuilder();
        Map<String, BigInteger> ranking = dbHandler.getRanking();
        int i = 1;
        Iterator<Map.Entry<String, BigInteger>> it = ranking.entrySet().iterator();
        while (it.hasNext() && i <= max) {
            Map.Entry<String, BigInteger> entry = it.next();
            description.append(String.format("%d. <@%s>, %d social credit%n", i, entry.getKey(), entry.getValue()));
            i++;
        }
        eb.setDescription(description);

        event.replyEmbeds(eb.build()).queue();
    }
}
