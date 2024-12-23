import cmd.*;
import db.DatabaseHandler;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cmd.CmdOption;

import java.util.*;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Bot extends ListenerAdapter {
    private static final Dotenv dotenv = Dotenv.load();
    private static final Logger log = LoggerFactory.getLogger(Bot.class);
    private static final DatabaseHandler dbHandler = new DatabaseHandler("/db/credit.csv");
    private static Map<CmdData, Cmd> cmdMap = Map.of(
            new CmdData("credit", "Add or subtract social credit from a user",
                    List.of(new CmdOption(USER, "user", "The user to add or subtract credit from", true),
                            new CmdOption(INTEGER, "amount", "Amount of credit to add or subtract", true)),
                    true, false), new Credit(dbHandler),

            new CmdData("leaderboard", "View social credit rankings",
                    List.of(new CmdOption(INTEGER, "max", "Number of users to display. If not provided, defaults to 10", false)),
                    true, true), new Leaderboard(dbHandler),

            new CmdData("profile", "View a user's social credit stats",
                    List.of(new CmdOption(USER, "user", "The user to view", true)),
                    true, true), new Profile(dbHandler),

            new CmdData("say", "Makes the bot say what you tell it to",
                    List.of(new CmdOption(STRING, "content", "What the bot should say", true),
                            new CmdOption(CHANNEL, "channel", "Channel to send message in. If not provided, defaults to current channel", false)),
                    true, false), new Say()
    );

    public static void main(String[] args) {
        JDA jda = JDABuilder.createLight(dotenv.get("TOKEN"), EnumSet.noneOf(GatewayIntent.class)) // slash commands don't need any intents
                .addEventListeners(new Bot())
                .build();

        // These commands might take a few minutes to be active after creation/update/delete
        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(getSlashCommands());

        // Send the new set of commands to discord, this will override any existing global commands with the new set provided here
        commands.queue();
        log.info("Registered slash commands");
    }

    private static List<SlashCommandData> getSlashCommands() {
        List<SlashCommandData> result = new ArrayList<>();
        cmdMap.keySet().forEach(key -> {
            SlashCommandData slashCmd = Commands.slash(key.name(), key.description());
            key.options().forEach(option ->
                    slashCmd.addOption(option.optionType(), option.name(), option.description(), option.required()));
            slashCmd.setGuildOnly(key.guildOnly());
            result.add(slashCmd);
        });
        return result;
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
        // these commands should only work in guilds:
        if (event.getGuild() == null) {
            event.reply("My slash commands only work in servers!").setEphemeral(true).queue();
            return;
        }

        // ensure the commands will only work in OC STEM or the test server
        String serverId = event.getGuild().getId();
        if (!serverId.equals(dotenv.get("OC-STEM_ID")) && !serverId.equals(dotenv.get("TEST_SERVER_ID"))) {
            event.reply("NOOBY NOOB").queue();
            return;
        }

        boolean found = false;
        for (Map.Entry<CmdData, Cmd> entry : cmdMap.entrySet()) {
            CmdData cmdData = entry.getKey();
            Cmd cmd = entry.getValue();

            if (event.getName().equals(cmdData.name())) {
                found = true;

                // ensure only me and Bhaia can use the bot, except for whitelisted commands (which can be used by anyone)
                String userId = event.getUser().getId();
                if (!userId.equals(dotenv.get("OWNER_ID")) && !userId.equals(dotenv.get("DEV_ID")) && !cmdData.whitelisted()) {
                    event.reply("NOOB").queue();
                    return;
                }

                cmd.run(event);
            }
        }
        if (!found)
            event.reply("I can't handle this command at the moment :c").setEphemeral(true).queue();
    }
}
