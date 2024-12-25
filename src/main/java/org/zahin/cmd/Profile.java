package org.zahin.cmd;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.db.DatabaseHandler;
import org.zahin.db.UserProfile;
import org.zahin.util.Util;

import java.time.Instant;

public class Profile extends Cmd {
    private final DatabaseHandler dbHandler;
    private final Dotenv dotenv;

    public Profile(DatabaseHandler dbHandler, Dotenv dotenv) {
        this.dbHandler = dbHandler;
        this.dotenv = dotenv;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        User user = event.getOption("user").getAsUser();
        profile(event, user);
    }

    private void profile(SlashCommandInteractionEvent event, User user) {
        UserProfile profile = dbHandler.read(user.getId());

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(dotenv.get("BOT_NAME"));
        eb.setTitle(String.format("@%s's profile", user.getName()));
        eb.setDescription(String.format("""
                User: <@%s>
                ```java
                Social Credit Balance:   %d
                Number of Credit Gains:  %d
                Number of Credit Losses: %d
                ```""", profile.id(), profile.balance(), profile.numGain(), profile.numLoss()));
//        eb.setFooter("Try /credit");
        eb.setTimestamp(Instant.now());

        String pfp = user.getAvatarUrl();
        int colour = Util.mostCommonColour(Util.urlToImg(pfp));
        eb.setThumbnail(pfp);
        eb.setColor(colour);

        event.replyEmbeds(eb.build()).queue();
    }
}
