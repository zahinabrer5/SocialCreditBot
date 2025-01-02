package org.zahin.cmd;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.zahin.db.DatabaseHandler;
import org.zahin.db.UserProfile;
import org.zahin.util.CustomEmbed;
import org.zahin.util.Util;

import java.awt.*;

public class Profile extends Cmd {
    private final DatabaseHandler dbHandler;
    private final Dotenv dotenv;

    public Profile(DatabaseHandler dbHandler, Dotenv dotenv) {
        this.dbHandler = dbHandler;
        this.dotenv = dotenv;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        OptionMapping userOption = event.getOption("user");
        User user = event.getUser();
        if (userOption != null)
            user = userOption.getAsUser();
        profile(event, user);
    }

    private void profile(SlashCommandInteractionEvent event, User user) {
        UserProfile profile = dbHandler.read(user.getId());

        CustomEmbed embed = new CustomEmbed(dotenv);
        embed.setTitle(String.format("@%s's profile", user.getName()));
        embed.setDescription(String.format("""
                User: <@%s>
                ```go
                \u200B
                Social Credit Balance:   %d
                Number of Credit Gains:  %d
                Number of Credit Losses: %d
                ```""", profile.id(), profile.balance(), profile.numGain(), profile.numLoss()));

        String pfp = user.getAvatarUrl();
        int colour = Color.DARK_GRAY.getRGB();
        if (pfp != null) {
            colour = Util.mostCommonColour(Util.urlToImage(pfp));
            embed.setThumbnail(pfp);
        }
        embed.setColor(colour);

        event.replyEmbeds(embed.build()).queue();
    }
}
