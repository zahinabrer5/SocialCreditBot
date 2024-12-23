package cmd;

import db.DatabaseHandler;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.math.BigInteger;
import java.time.Instant;
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

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(dotenv.get("BOT_NAME"));
        eb.setTitle("Top "+max+" Best Citizens of "+dotenv.get("MAIN_SERVER"));
        eb.setFooter("Try /profile");
        eb.setTimestamp(Instant.now());
        eb.setColor(0xfcdb00);

        StringBuilder description = new StringBuilder();
        Map<String, BigInteger> ranking = dbHandler.getRanking();
        int i = 1;
        Iterator<Map.Entry<String, BigInteger>> it = ranking.entrySet().iterator();
        while (it.hasNext() && i <= max) {
            Map.Entry<String, BigInteger> entry = it.next();
            description.append(String.format("%d. <@%s>: **%d** social credit%n", i, entry.getKey(), entry.getValue()));
            i++;
        }
        eb.setDescription(description);

        event.replyEmbeds(eb.build()).queue();
    }
}
