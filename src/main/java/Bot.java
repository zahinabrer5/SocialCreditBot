import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Bot extends ListenerAdapter {
    private static final Dotenv dotenv = Dotenv.load();
    private static final Logger log = LoggerFactory.getLogger(Bot.class);
    private final DatabaseHandler dbHandler = new DatabaseHandler("credit.csv");

    public static void main(String[] args) {
        JDA jda = JDABuilder.createLight(dotenv.get("TOKEN"), EnumSet.noneOf(GatewayIntent.class)) // slash commands don't need any intents
                .addEventListeners(new Bot())
                .build();

        // These commands might take a few minutes to be active after creation/update/delete
        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(
                Commands.slash("credit", "Add or subtract social credit from a user")
                        .addOption(USER, "user", "The user to add or subtract credit from", true)
                        .addOption(INTEGER, "amount", "Amount of credit to add or subtract", true)
                        .setGuildOnly(true),

                Commands.slash("leaderboard", "View social credit rankings")
                        .addOption(INTEGER, "max", "Number of users to display", true)
                        .setGuildOnly(true),

                Commands.slash("say", "Makes the bot say what you tell it to")
                        .addOption(STRING, "content", "What the bot should say", true)
                        .addOption(CHANNEL, "channel", "Channel to send message in", true)
                        .setGuildOnly(true)
        );

        // Send the new set of commands to discord, this will override any existing global commands with the new set provided here
        commands.queue();
        log.info("Registered slash commands");
    }

    // Database
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        dbHandler.loadDatabase();
        log.info("Ready! Loaded Database");
    }

    @Override
    public void onSessionResume(@NotNull SessionResumeEvent event) {
//        dbHandler.loadDatabase();
//        log.info("Loaded Database due to Session Resume");
    }

    @Override
    public void onSessionRecreate(@NotNull SessionRecreateEvent event) {
//        dbHandler.loadDatabase();
//        log.info("Loaded Database due to Session Recreate");
    }

    @Override
    public void onSessionDisconnect(@NotNull SessionDisconnectEvent event) {
//        dbHandler.saveDatabase();
//        log.info("Saved Database due to Session Disconnect");
    }

    @Override
    public void onSessionInvalidate(@NotNull SessionInvalidateEvent event) {
//        dbHandler.saveDatabase();
//        dbHandler.loadDatabase();
//        log.info("Saved and (re)loaded Database due to Session Invalidate");
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
//        dbHandler.saveDatabase();
//        log.info("Saved Database due to Shutdown");
    }

    // Slash Commands
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        List<String> cmdWhitelist = List.of("leaderboard", "profile");

        // ensure only me and Bhaia can use the bot, except for whitelisted commands (which can be used by anyone)
        String userId = event.getUser().getId();
        if (!userId.equals(dotenv.get("OWNER_ID")) && !userId.equals(dotenv.get("DEV_ID")) && !cmdWhitelist.contains(event.getName())) {
            event.reply("NOOB").queue();
            return;
        }

        // these commands should only work in guilds:
        if (event.getGuild() == null) {
            event.reply("Slash commands only work in servers!").setEphemeral(true).queue();
            return;
        }

        String serverId = event.getGuild().getId();
        if (!serverId.equals(dotenv.get("OC-STEM_ID")) && !serverId.equals(dotenv.get("TEST_SERVER_ID"))) {
            event.reply("NOOBY NOOB").queue();
            return;
        }

        switch (event.getName()) {
            case "credit" -> {
                Member member = event.getOption("user").getAsMember(); // the "user" option is required, so it doesn't need a null-check here
                User user = event.getOption("user").getAsUser();
                long amount = event.getOption("amount").getAsLong();
                credit(event, user, member, amount);
            }

            case "leaderboard" -> leaderboard(event, event.getOption("max").getAsInt());

            case "say" -> say(event, event.getOption("content").getAsString(), event.getOption("channel").getAsChannel()); // content is required so no null-check here

            default -> event.reply("I can't handle that command right now :(").setEphemeral(true).queue();
        }
    }

    private void credit(SlashCommandInteractionEvent event, User user, Member member, long amount) {
        if (amount == 0) {
            event.reply("Amount cannot be 0 (zero)!").setEphemeral(true).queue();
            return;
        }

        String userId = user.getId();
        dbHandler.update(userId, amount);
        BigInteger balance = dbHandler.read(userId).balance();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor("People's Republic of OC STEM");
        eb.setTitle(Util.fmt.format(amount)+" social credit!");
        eb.setDescription(String.format("<@%s> now has %d social credit", userId, balance));
        eb.setFooter("Try /leaderboard");
//        eb.setTimestamp(LocalDateTime.now(ZoneId.of("America/Toronto")));

        String img = "https://i.imgur.com/HsM6YU1.png";
        Color colour = new Color(0x2eb33e);
        if (amount < 0) {
            img = "https://i.imgur.com/l4sQ8lV.png";
            colour = new Color(0xff0000);
        }
        eb.setThumbnail(img);
        eb.setColor(colour);

        event.replyEmbeds(eb.build()).queue();
    }

    private void leaderboard(SlashCommandInteractionEvent event, int max) {
        if (max < 1) {
            event.reply("`max` option must be at least 1!").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor("People's Republic of OC STEM");
        eb.setTitle("Top "+max+" Best Citizens of OC STEM");
        eb.setFooter("Try /profile");
        eb.setColor(new Color(0xfcdb00));

        StringBuilder description = new StringBuilder();
        Map<String, BigInteger> ranking = dbHandler.getRanking();
        int i = 1;
        Iterator<Entry<String, BigInteger>> it = ranking.entrySet().iterator();
        while (it.hasNext() && i <= max) {
            Entry<String, BigInteger> entry = it.next();
            description.append(String.format("%d. <@%s>, %d social credit%n", i, entry.getKey(), entry.getValue()));
            i++;
        }
        eb.setDescription(description);

        event.replyEmbeds(eb.build()).queue();
    }

    private void say(SlashCommandInteractionEvent event, String content, Channel channel) {
        if (channel.getType() != ChannelType.TEXT) {
            event.reply("Channel must be a text channel!").setEphemeral(true).queue();
            return;
        }

        event.getGuild().getTextChannelById(channel.getId()).sendMessage(content).queue();
        event.reply(String.format("Sent message successfully to <#%s>", channel.getId())).setEphemeral(true).queue();
    }
}
