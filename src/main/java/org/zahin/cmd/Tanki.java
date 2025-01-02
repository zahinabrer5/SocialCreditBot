package org.zahin.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.zahin.util.CustomEmbed;
import org.zahin.util.HttpResponseType;
import org.zahin.util.Util;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

record Rating(int position, int value) {
}

record WeeklyRatings(Rating crystals, Rating efficiency, Rating golds, Rating score) {
}

record Supply(String id, String imageUrl, String name, int usages) {
}

record GameMode(String name, int scoreEarned, long timePlayed, String type) {
}

record Equipment(int grade, String id, String imageUrl, String name, List<String> properties, int scoreEarned,
                 long timePlayed) {
}

record Present(int count, String imageUrl, String name, String prototypeId) {
}

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
) {
}

record TankiRatingsApiResponse(ResponseJsonObj response, HttpResponseType responseType) {
}

public class Tanki extends Cmd implements Runnable {
    private static final String[] rankNames = {"Recruit", "Private", "Gefreiter", "Corporal", "Master Corporal", "Sergeant", "Staff Sergeant", "Master Sergeant", "First Sergeant", "Sergeant-Major", "Warrant Officer 1", "Warrant Officer 2", "Warrant Officer 3", "Warrant Officer 4", "Warrant Officer 5", "Third Lieutenant", "Second Lieutenant", "First Lieutenant", "Captain", "Major", "Lieutenant Colonel", "Colonel", "Brigadier", "Major General", "Lieutenant General", "General", "Marshal", "Field Marshal", "Commander", "Generalissimo", "Legend (1)"};
    private final ObjectMapper objectMapper;
    private final Dotenv dotenv;
    private final ScheduledExecutorService scheduler;
    private SlashCommandInteractionEvent event;

    public Tanki(ObjectMapper objectMapper, Dotenv dotenv, ScheduledExecutorService scheduler) {
        this.objectMapper = objectMapper;
        this.dotenv = dotenv;
        this.scheduler = scheduler;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        this.event = event;
        scheduler.submit(this);
    }

    @Override
    public void run() {
        tanki();
    }

    private void tanki() {
        String player = event.getOption("p").getAsString();
        String urlStr = "https://ratings.tankionline.com/api/eu/profile/?user=" + player + "&lang=en";
        URL url;
        try {
            url = new URI(urlStr).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }

        int code;
        try {
            code = ((HttpURLConnection) url.openConnection()).getResponseCode();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (code != HttpURLConnection.HTTP_OK) {
            errorEmbed(event, code);
            return;
        }

        TankiRatingsApiResponse apiResponse;
        try {
            apiResponse = objectMapper.readValue(url, TankiRatingsApiResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (apiResponse.response() == null || !apiResponse.responseType().status().equals("OK")) {
            errorEmbed(event, apiResponse.responseType().statusCode());
            return;
        }

        sendEmbeds(apiResponse.response());
    }

    private void sendEmbeds(ResponseJsonObj resp) {
        CustomEmbed embed = new CustomEmbed(dotenv);
        embed.setTitle(getRank(resp.rank()) + " " + resp.name());
        embed.setUrl("https://ratings.tankionline.com/en/user/" + resp.name());
        embed.setDescription(String.format("%s / %s XP", Util.thousandsSep(resp.score()), Util.thousandsSep(resp.scoreNext())));
        embed.setColor(0x036530);

        CustomEmbed weeklyRatings = new CustomEmbed(dotenv);
        weeklyRatings.addField("__Weekly Ratings__", getWeeklyRatingsTable(resp), false);
        weeklyRatings.setFooter("View this embed in landscape mode if on mobile | See next embed for rest of stats");
        weeklyRatings.setColor(0x036530);

        if (resp.hasPremium()) {
            embed.setColor(0xfbd003);
            weeklyRatings.setColor(0xfbd003);
        }

        embed.addField("", "**__Profile__**", false);
        embed.addField("Kills", Util.thousandsSep(resp.kills()), true);
        embed.addField("Deaths", Util.thousandsSep(resp.deaths()), true);
        embed.addField("K/D", Util.twoDecFmt.format(resp.kills() * 1.0 / resp.deaths()), true);
        embed.addField("Hours in game", Util.thousandsSep(getHours(resp.modesPlayed())), true);
        long eff = Math.round(resp.rating().efficiency().value() / 100.0);
        embed.addField("Efficiency", eff < 1 ? "-" : Util.thousandsSep(eff), true);
        embed.addField("Total Crystals Earned", Util.thousandsSep(resp.earnedCrystals()), true);
        embed.addField("Golds Caught", Util.thousandsSep(resp.caughtGolds()), true);
        embed.addField("Total Supplies Used", Util.thousandsSep(getTotalSuppliesUsed(resp.suppliesUsage())), true);
        embed.addField("Gear Score", "**" + resp.gearScore() + "**", true);

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

        int rank = Math.min(resp.rank(), 31);
        String file = String.format("/img/ranks/Icons%s_%01d.png", resp.hasPremium() ? "Premium" : "Normal", rank);
        InputStream rankIcon = getClass().getResourceAsStream(file);
        embed.setThumbnail("attachment://rank.png");
        event.deferReply().queue();
        event.getHook().editOriginalAttachments(FileUpload.fromData(rankIcon, "rank.png")).queue();
        event.getHook().editOriginalEmbeds(weeklyRatings.build(), embed.build()).queue();
    }

    private void errorEmbed(SlashCommandInteractionEvent event, int code) {
        CustomEmbed embed = new CustomEmbed(dotenv);
        embed.setTitle("Could not fetch player stats!");
        embed.setDescription("Make sure the player actually exists...");
        embed.setImage("https://http.cat/" + code);
        embed.setColor(Color.BLACK);
        event.replyEmbeds(embed.build()).queue();
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
        // get the unique turrets / hulls & zero their integral values (scoreEarned & timePlayed)
        List<Equipment> uniq = equipment.stream().distinct().toList();
        Map<String, Equipment> map = getEquipmentStatsMap(equipment, uniq);

        Equipment favEq = equipment.getFirst();
        for (Map.Entry<String, Equipment> entry : map.entrySet()) {
            Equipment eq = entry.getValue();

            if (eq.timePlayed() > favEq.timePlayed())
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

    @NotNull
    private static Map<String, Equipment> getEquipmentStatsMap(List<Equipment> equipment, List<Equipment> uniq) {
        Map<String, Equipment> map = new HashMap<>();
        for (Equipment eq : uniq) {
            Equipment zeroed = new Equipment(eq.grade(), eq.id(), eq.imageUrl(), eq.name(), eq.properties(), 0, 0);
            map.put(zeroed.name(), zeroed);
        }

        // total turret / hull integral values
        for (Equipment eq : equipment) {
            Equipment eqCurr = map.get(eq.name());
            Equipment eqNew = new Equipment(eq.grade(), eq.id(), eq.imageUrl(), eq.name(), eq.properties(),
                    eqCurr.scoreEarned() + eq.scoreEarned(), eqCurr.timePlayed() + eq.timePlayed());
            map.put(eqNew.name(), eqNew);
        }

        return map;
    }

    private String getWeeklyRatingsTable(ResponseJsonObj resp) {
        WeeklyRatings weeklyRatings = resp.rating();
        if (weeklyRatings == null)
            return "No weekly ratings; there might be previous ratings";

        String[] xpRow = getRatingRow(weeklyRatings.score(), false);
        String[] gbRow = getRatingRow(weeklyRatings.golds(), false);
        String[] crRow = getRatingRow(weeklyRatings.crystals(), false);
        String[] efRow = getRatingRow(weeklyRatings.efficiency(), true);

        WeeklyRatings prevWeeklyRatings = resp.previousRating();
        String prevXp = "-", prevGb = "-", prevCr = "-", prevEf = "-";
        if (prevWeeklyRatings != null) {
            prevXp = getPrevRating(prevWeeklyRatings.score(), false);
            prevGb = getPrevRating(prevWeeklyRatings.golds(), false);
            prevCr = getPrevRating(prevWeeklyRatings.crystals(), false);
            prevEf = getPrevRating(prevWeeklyRatings.efficiency(), true);
        }

        return String.format("""
                        ```javascript
                        \u200B
                        Rating     |      Place |      Value | Previously
                        -------------------------------------------------
                        Experience | %10s | %10s | %10s
                        Gold Boxes | %10s | %10s | %10s
                        Crystals   | %10s | %10s | %10s
                        Efficiency | %10s | %10s | %10s
                        ```""",
                xpRow[0], xpRow[1], prevXp,
                gbRow[0], gbRow[1], prevGb,
                crRow[0], crRow[1], prevCr,
                efRow[0], efRow[1], prevEf);
    }

    private String[] getRatingRow(Rating rating, boolean ef) {
        String pos = "-", val = "-";
        if (rating != null) {
            if (rating.position() > 0)
                pos = ("#" + Util.thousandsSep(rating.position()));
            if (rating.value() > 0) {
                long valAsLong = ef ? Math.round(rating.value() / 100.0) : rating.value();
                val = Util.thousandsSep(valAsLong);
            }
        }
        return new String[]{pos, val};
    }

    private String getPrevRating(Rating prevRating, boolean ef) {
        String prev = "-";
        if (prevRating != null) {
            if (prevRating.value() < 1)
                return prev;
            long prevAsLong = ef ? Math.round(prevRating.value() / 100.0) : prevRating.value();
            prev = Util.thousandsSep(prevAsLong);
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
            return rankNames[rawRank - 1];

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
