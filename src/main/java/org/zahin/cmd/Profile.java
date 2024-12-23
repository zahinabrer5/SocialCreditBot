package org.zahin.cmd;

import org.zahin.db.DatabaseHandler;
import org.zahin.db.UserProfile;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.util.Util;

import java.awt.*;
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
        eb.setFooter("Try /credit");
        eb.setTimestamp(Instant.now());

        String pfp = user.getAvatarUrl();
        int colour = mostCommonColour(Util.urlToImg(pfp));
        eb.setThumbnail(pfp);
        eb.setColor(colour);

        event.replyEmbeds(eb.build()).queue();
    }

    private int mostCommonColour(Image img) {
        // my bad algo:
        /*
        Map<Color, Integer> colourFreq = new LinkedHashMap<>();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color colour = new Color(img.getRGB(x, y));
                colourFreq.put(colour, colourFreq.getOrDefault(colour, 0)+1);
            }
        }

        double totalOccurences = colourFreq.values().stream()
                .mapToInt(i -> i) // automatically unboxes Integer type to primitive int
                .sum();

        List<Color> dominantColours = colourFreq.keySet().stream()
                .filter(i -> colourFreq.get(i) / totalOccurences >= 0.1)
                .toList();

        if (dominantColours.isEmpty())
            return Color.GRAY;

        return dominantColours.stream().reduce(dominantColours.getFirst(), this::averageColour);
        */

        Image scaled = img.getScaledInstance(1, 1, Image.SCALE_REPLICATE);
        return Util.toBufferedImage(scaled).getRGB(0, 0);
    }

    // https://stackoverflow.com/a/29576746/21405641
    /*
    private Color averageColour(Color a, Color b) {
        double r1 = a.getRed(), r2 = b.getRed();
        double g1 = a.getGreen(), g2 = b.getGreen();
        double b1 = a.getBlue(), b2 = b.getBlue();
        return new Color(roundSqrt((r1*r1+r2*r2)/2), roundSqrt((g1*g1+g2*g2)/2), roundSqrt((b1*b1+b2*b2)/2));
    }

    private int roundSqrt(double x) {
        return (int)Math.round(Math.sqrt(x));
    }
    */
}
