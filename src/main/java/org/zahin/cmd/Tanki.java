package org.zahin.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.zahin.util.CustomEmbed;
import org.zahin.util.HttpResponseType;
import org.zahin.util.Util;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

record Rating(int position, int value) {}

record WeeklyRatings(Rating crystals, Rating efficiency, Rating golds, Rating score) {}

record Supply(String id, String imageUrl, String name, int usages) {}

record GameMode(String name, int scoreEarned, long timePlayed, String type) {}

record Equipment(int grade, String id, String imageUrl, String name, List<String> properties, int scoreEarned, long timePlayed) {}

record Present(int count, String imageUrl, String name, String prototypeId) {}

record ResponseJsonObj(
        int caughtGolds,
        int deaths,
        List<Equipment> dronesPlayed,
        int earnedCrystals,
        int gearScore,
        boolean hasPremium,
        List<Equipment> hullsPlayed,
        int kills,
        List<GameMode> modesPlayed,
        String name,
        List<Equipment> paintsPlayed,
        List<Present> presents,
        WeeklyRatings previousRating,
        int rank,
        WeeklyRatings rating,
        List<Equipment> resistanceModules,
        int score,
        int scoreBase,
        int scoreNext,
        List<Supply> suppliesUsage,
        List<Equipment> turretsPlayed
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
            event.replyEmbeds(embed.build()).queue();
        }
        else {
            ResponseJsonObj resp = apiResponse.response();
            embed.setTitle(getRank(resp.rank())+" "+resp.name());
            embed.setUrl("https://ratings.tankionline.com/en/user/"+resp.name());
            embed.setDescription(String.format("%s / %s XP", Util.thousandsSep(resp.score()), Util.thousandsSep(resp.scoreNext())));
            embed.setColor(0x036530);

            CustomEmbed weeklyRatings = new CustomEmbed(dotenv);
            weeklyRatings.addField("__Weekly Ratings__", getWeeklyRatingsTable(resp), false);
            weeklyRatings.setFooter("See next message (embed) for rest of stats");
            weeklyRatings.setColor(0x036530);

            if (resp.hasPremium()) {
                embed.setColor(0xfbd003);
                weeklyRatings.setColor(0xfbd003);
            }

            event.replyEmbeds(weeklyRatings.build()).queue();

            embed.addField("", "**__Profile__**", false);
            embed.addField("Kills", Util.thousandsSep(resp.kills()), true);
            embed.addField("Deaths", Util.thousandsSep(resp.deaths()), true);
            embed.addField("K/D", Util.twoDecFmt.format(resp.kills()*1.0/resp.deaths()), true);
            embed.addField("Hours in game", Util.thousandsSep(getHours(resp.modesPlayed())), true);
            int eff = resp.rating().efficiency().value()/100;
            embed.addField("Efficiency", eff < 1 ? "-" : Util.thousandsSep(eff), true);
            embed.addField("Total Crystals Earned", Util.thousandsSep(resp.earnedCrystals()), true);
            embed.addField("Golds Caught", Util.thousandsSep(resp.caughtGolds()), true);
            embed.addField("Total Supplies Used", Util.thousandsSep(getTotalSuppliesUsed(resp.suppliesUsage())), true);
            embed.addField("Gear Score", "**"+resp.gearScore()+"**", true);

            embed.addField("", "**__Most Used Equipment/Gear__**", false);
            if (!resp.turretsPlayed().isEmpty())
                embed.addField("Turret", getFavEquipment(resp.turretsPlayed()), true);
            if (!resp.hullsPlayed().isEmpty())
                embed.addField("Hull", getFavEquipment(resp.hullsPlayed()), true);
            if (!resp.dronesPlayed().isEmpty())
                embed.addField("Drone", getFavEquipment(resp.dronesPlayed()), true);
            if (!resp.resistanceModules().isEmpty())
                embed.addField("Module", getFavEquipment(resp.resistanceModules()), true);
            if (!resp.paintsPlayed().isEmpty())
                embed.addField("Paint", getFavEquipment(resp.paintsPlayed()), true);
            if (!resp.suppliesUsage().isEmpty())
                embed.addField("Supply", getFavSupply(resp.suppliesUsage()), true);

            embed.addField("", "**__Other__**", false);
            if (!resp.modesPlayed().isEmpty())
                embed.addField("Favourite Game Mode", getFavMode(resp.modesPlayed()), true);
            if (!resp.presents().isEmpty())
                embed.addField("Gifts", getGiftsInfo(resp.presents()), true);

            embed.setFooter("View on desktop for better embed formatting");

            MessageChannel channel = event.getMessageChannel();
            String file = String.format("/img/ranks/Icons%s_%01d.png", resp.hasPremium() ? "Premium" : "Normal", resp.rank());
            InputStream rankIcon = getClass().getResourceAsStream(file);
            embed.setThumbnail("attachment://rank.png");
            channel.sendFiles(FileUpload.fromData(rankIcon, "rank.png")).setEmbeds(embed.build()).queue();
        }
    }

    private String getGiftsInfo(List<Present> presents) {
        int total = presents.stream().mapToInt(Present::count).sum();

        Present popularGift = presents.getFirst();
        for (Present present : presents)
            if (present.count() > popularGift.count())
                popularGift = present;

        String name = popularGift.name();
        int count = popularGift.count();
        return String.format("%s - %d received, %d total gifts received", name, count, total);
    }

    private String getFavMode(List<GameMode> gameModes) {
        GameMode favMode = gameModes.getFirst();
        for (GameMode mode : gameModes)
            if (mode.timePlayed() > favMode.timePlayed())
                favMode = mode;

        String name = favMode.name();
        String type = favMode.type();
        double hours = favMode.timePlayed() / 3600.0 / 1000;
        String xp = Util.thousandsSep(favMode.scoreEarned());
        return String.format("%s (%s) - %s hours, %s XP", name, type, Util.oneDecFmt.format(hours), xp);
    }

    private String getFavEquipment(List<Equipment> equipment) {
        Map<Equipment, Long> timePlayedPerEq = new HashMap<>();
        for (Equipment eq : equipment)
            timePlayedPerEq.put(eq, timePlayedPerEq.getOrDefault(eq, 0L)+eq.timePlayed());

        Equipment favEq = equipment.getFirst();
        for (Map.Entry<Equipment, Long> entry : timePlayedPerEq.entrySet()) {
            Equipment eq = entry.getKey();
            long timePlayedMillis = entry.getValue();

            if (timePlayedMillis > favEq.timePlayed())
                favEq = eq;
        }

        String name = favEq.name();
        String hours = Util.oneDecFmt.format(favEq.timePlayed() / 3600.0 / 1000);
        String xp = Util.thousandsSep(favEq.scoreEarned());

        if (equipment.getFirst().grade() < 0)
            return String.format("%s - %s hours, %s XP", name, hours, xp);

        int grade = equipment.stream()
                .filter(eq -> eq.name().equals(name))
                .mapToInt(Equipment::grade)
                .max().getAsInt() + 1;
        return String.format("%s Mk%d - %s hours, %s XP", name, grade, hours, xp);
    }

    private String getWeeklyRatingsTable(ResponseJsonObj resp) {
        WeeklyRatings weeklyRatings = resp.rating();
        if (weeklyRatings == null)
            return "No weekly ratings; there might be previous ratings";

        String[] xpRow = getRatingRow(weeklyRatings.score());
        String[] gbRow = getRatingRow(weeklyRatings.golds());
        String[] crRow = getRatingRow(weeklyRatings.crystals());
        String[] efRow = getRatingRow(weeklyRatings.efficiency());

        WeeklyRatings prevWeeklyRatings = resp.previousRating();
        String prevXp = "-", prevGb = "-", prevCr = "-", prevEf = "-";
        if (prevWeeklyRatings != null) {
            prevXp = getPrevRating(prevWeeklyRatings.score());
            prevGb = getPrevRating(prevWeeklyRatings.golds());
            prevCr = getPrevRating(prevWeeklyRatings.crystals());
            prevEf = getPrevRating(prevWeeklyRatings.efficiency());
        }

        return String.format("""
                    ```javascript
                    Rating     |      Place |      Value | Previously
                    -------------------------------------------------
                    Experience | %10s | %10s | %10s
                    Gold Boxes | %10s | %10s | %10s
                    Crystals   | %10s | %10s | %10s
                    Efficiency | %10s | %10s | %10s
                    ```""", xpRow[0], xpRow[1], prevXp,
                            gbRow[0], gbRow[1], prevGb,
                            crRow[0], crRow[1], prevCr,
                            efRow[0], efRow[1], prevEf);
    }

    private String[] getRatingRow(Rating rating) {
        String pos = "-", val = "-";
        if (rating != null) {
            if (rating.position() > 0)
                pos = ("#"+Util.thousandsSep(rating.position()));
            if (rating.value() > 0)
                val = Util.thousandsSep(rating.value());
        }
        return new String[]{pos, val};
    }

    private String getPrevRating(Rating prevRating) {
        String prev = "-";
        if (prevRating != null) {
            if (prevRating.value() < 1)
                return prev;
            prev = Util.thousandsSep(prevRating.value());
        }
        return prev;
    }

    private long getHours(List<GameMode> modes) {
        if (modes.isEmpty())
            return 0;
        return Util.millisToHours(modes.stream().mapToLong(GameMode::timePlayed).sum());
    }

    private String getRank(int rawRank) {
        if (rawRank <= rankNames.length)
            return rankNames[rawRank-1];

        return "Legend " + (rawRank - rankNames.length + 1);
    }

    private int getTotalSuppliesUsed(List<Supply> supplies) {
        if (supplies.isEmpty())
            return 0;
        return supplies.stream().mapToInt(Supply::usages).sum();
    }

    private String getFavSupply(List<Supply> supplies) {
        Supply mostUsed = supplies.getFirst();
        for (Supply supply : supplies)
            if (supply.usages() > mostUsed.usages())
                mostUsed = supply;
        return String.format("%s - %s used", mostUsed.name(), Util.thousandsSep(mostUsed.usages()));
    }
}
