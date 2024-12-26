package org.zahin.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.util.CustomEmbed;
import org.zahin.util.HttpResponseType;
import org.zahin.util.Util;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

record Rating(int position, int value) {}

record Ratings(Rating crystals, Rating efficiency, Rating golds, Rating score) {}

record Supply(String id, String imageUrl, String name, int usages) {}

record GameMode(String name, int scoreEarned, long timePlayed, String type) {}

record ResponseJsonObj(
        int caughtGolds,
        int deaths,
        List<Object> dronesPlayed,
        int earnedCrystals,
        int gearScore,
        boolean hasPremium,
        List<Object> hullsPlayed,
        int kills,
        List<GameMode> modesPlayed,
        String name,
        List<Object> paintsPlayed,
        List<Object> presents,
        Object previousRating,
        int rank,
        Ratings rating,
        List<Object> resistanceModules,
        int score,
        int scoreBase,
        int scoreNext,
        List<Supply> suppliesUsage,
        List<Object> turretsPlayed
) {}

record TankiRatingsApiResponse(ResponseJsonObj response, HttpResponseType responseType) {}

public class Tanki extends Cmd {
    private final ObjectMapper objectMapper;
    private final Dotenv dotenv;
    private static final String[] rankNames = { "Recruit", "Private", "Gefreiter", "Corporal", "Master Corporal", "Sergeant", "Staff Sergeant", "Master Sergeant", "First Sergeant", "Sergeant-Major", "Warrant Officer 1", "Warrant Officer 2", "Warrant Officer 3", "Warrant Officer 4", "Warrant Officer 5", "Third Lieutenant", "Second Lieutenant", "First Lieutenant", "Captain", "Major", "Lieutenant Colonel", "Colonel", "Brigadier", "Major General", "Lieutenant General", "General", "Marshal", "Field Marshal", "Commander", "Generalissimo", "Legend (1)" };

    public Tanki(ObjectMapper objectMapper, Dotenv dotenv) {
        this.objectMapper = objectMapper;
        this.dotenv = dotenv;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        String player = event.getOption("p").getAsString();
        tanki(event, player);
    }

    private void tanki(SlashCommandInteractionEvent event, String player) {
        String url = "https://ratings.tankionline.com/api/eu/profile/?user="+player+"&lang=en";
        TankiRatingsApiResponse apiResponse;
        try {
            apiResponse = objectMapper.readValue(new URI(url).toURL(), TankiRatingsApiResponse.class);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

        CustomEmbed embed = new CustomEmbed(dotenv);
        if (apiResponse.response() == null || !apiResponse.responseType().status().equals("OK")) {
            embed.setTitle("Could not fetch player stats!");
            embed.setDescription("Make sure the player actually exists...");
            embed.setImage("https://http.cat/"+apiResponse.responseType().statusCode());
            embed.setColor(Color.BLACK);
        }
        else {
            ResponseJsonObj resp = apiResponse.response();
            embed.setTitle(getRank(resp.rank())+" "+resp.name());
            embed.setUrl("https://ratings.tankionline.com/en/user/"+resp.name());
            embed.setDescription(String.format("%,d / %,d XP", resp.score(), resp.scoreNext()));
            embed.setColor(0x036530);
            embed.addField("Kills", Util.thousandsSep(resp.kills()), true);
            embed.addField("Deaths", Util.thousandsSep(resp.deaths()), true);
            embed.addField("K/D", Util.decFmt.format(resp.kills()*1.0/resp.deaths()), true);
            embed.addField("Hours in game", Util.thousandsSep(getHours(resp.modesPlayed())), true);
            embed.addField("Efficiency", Util.thousandsSep(resp.rating().efficiency().value()/100), true);
            embed.addField("Total Crystals Earned", Util.thousandsSep(resp.earnedCrystals()), true);
            embed.addField("Golds Caught", Util.thousandsSep(resp.caughtGolds()), true);
            embed.addField("Supplies Used", Util.thousandsSep(getSuppliesUsed(resp.suppliesUsage())), true);
            embed.addField("Gear Score", "**"+resp.gearScore()+"**", true);

            if (resp.hasPremium()) {
                embed.setColor(0xfbd003);
            }
        }
        event.replyEmbeds(embed.build()).queue();
    }

    private long getHours(List<GameMode> modes) {
        return Math.round((modes.stream().mapToLong(GameMode::timePlayed).sum()) / 3600.0 / 1000);
    }

    private String getRank(int rawRank) {
        if (rawRank <= rankNames.length)
            return rankNames[rawRank-1];

        return "Legend " + (rawRank - rankNames.length + 1);
    }

    private int getSuppliesUsed(List<Supply> supplies) {
        return supplies.stream().mapToInt(Supply::usages).sum();
    }
}
