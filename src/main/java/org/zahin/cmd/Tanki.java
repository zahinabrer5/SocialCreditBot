package org.zahin.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;

enum HttpResponseType {
    OK(200),
    NOT_FOUND(404);

    private final int statusCode;

    HttpResponseType(int statusCode) {
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}

record ResponseJsonObj(
        int caughtGolds,
        int deaths,
        List<Object> dronesPlayed,
        int earnedCrystals,
        int gearScore,
        boolean hasPremium,
        List<Object> hullsPlayed,
        int kills,
        List<Object> modesPlayed,
        String name,
        List<Object> paintsPlayed,
        List<Object> presents,
        Object previousRating,
        int rank,
        Object rating,
        List<Object> resistanceModules,
        int score,
        int scoreBase,
        int scoreNext,
        List<Object> suppliesUsage,
        List<Object> turretsPlayed
) {}

record TankiRatingsApiResponse(ResponseJsonObj response, HttpResponseType responseType) {}

public class Tanki extends Cmd {
    private final ObjectMapper objectMapper;

    public Tanki(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        String player = event.getOption("player").getAsString();
        tanki(event, player);
    }

    private void tanki(SlashCommandInteractionEvent event, String player) {
        String url = "https://ratings.tankionline.com/api/eu/profile/?user="+player+"&lang=en";
        System.out.println(url);
    }
}
