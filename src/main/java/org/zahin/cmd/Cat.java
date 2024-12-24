package org.zahin.cmd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.util.Util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;

record CatApiRequest(List<Object> breeds, String id, String url, int width, int height) {}

public class Cat extends Cmd {
    Dotenv dotenv;

    public Cat(Dotenv dotenv) {
        this.dotenv = dotenv;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        cat(event);
    }

    private void cat(SlashCommandInteractionEvent event) {
        String url = "https://api.thecatapi.com/v1/images/search?api_key="+dotenv.get("CAT_API_KEY");
        ObjectMapper objectMapper = new ObjectMapper();
        List<CatApiRequest> request;
        try {
            request = objectMapper.readValue(new URI(url).toURL(), new TypeReference<>(){});
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String retrievedCatUrl = request.getFirst().url();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(dotenv.get("BOT_NAME"));
        eb.setTitle("Here's your cat:");
        eb.setFooter("Try /say");
        eb.setTimestamp(Instant.now());

        int colour = Util.mostCommonColour(Util.urlToImg(retrievedCatUrl));
        eb.setImage(retrievedCatUrl);
        eb.setColor(colour);

        event.replyEmbeds(eb.build()).queue();
    }
}
