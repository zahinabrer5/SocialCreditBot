package org.zahin.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.zahin.util.CustomEmbed;
import org.zahin.util.Util;

import java.io.IOException;
import java.io.InputStream;

public class Cat extends Cmd {
    private final Dotenv dotenv;
    private final ObjectMapper objectMapper;

    public Cat(Dotenv dotenv, ObjectMapper objectMapper) {
        this.dotenv = dotenv;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        cat(event);
    }

    private void cat(SlashCommandInteractionEvent event) {
        long start = System.nanoTime();
        String url = "https://api.thecatapi.com/v1/images/search?api_key="+dotenv.get("CAT_API_KEY");

        JsonNode node;
        try {
            node = objectMapper.readTree(Util.urlContentToString(url)).get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!node.has("url")) {
            event.reply("Something has gone horribly wrong... your cat is nowhere to be found!!! Perhaps try again :cat:").setEphemeral(true).queue();
            return;
        }

        String retrievedCatUrl = node.get("url").asText("");
        long stop = System.nanoTime();
        double elapsed = (stop - start) / 1_000_000.0;

        CustomEmbed embed = new CustomEmbed(dotenv);
        embed.setTitle("Cat(s) acquired:");
        // showing how long the operation took could be a security risk in a production environment...
        if (Boolean.parseBoolean(dotenv.get("ON_MAINTENANCE"))) {
            embed.setFooter("Cat(s) acquired in "+Util.twoDecFmt.format(elapsed)+" ms");
        }

        int colour = Util.mostCommonColour(Util.urlToImage(retrievedCatUrl));
        embed.setColor(colour);

        if (retrievedCatUrl.isEmpty()) {
            InputStream contingencyCat = getClass().getResourceAsStream("/img/tabby.jpg");
            embed.setImage("attachment://tabby.jpg");
            event.reply("Cat couldn't be found whatsoever! Here's a backup cat:").queue();
            event.getMessageChannel().sendFiles(FileUpload.fromData(contingencyCat, "tabby.jpg")).setEmbeds(embed.build()).queue();
            return;
        }
        embed.setImage(retrievedCatUrl);
        event.replyEmbeds(embed.build()).queue();
    }
}
