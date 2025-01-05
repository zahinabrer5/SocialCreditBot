package org.zahin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zahin.cmd.*;
import org.zahin.db.DatabaseHandler;
import org.zahin.db.DatabaseLoader;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Bot extends ListenerAdapter {
    public static Instant startTime;
    public static final Dotenv dotenv = Dotenv.load();
    public static final Random rand = new Random();
    public static final ZoneId tz = ZoneId.of("America/Montreal");
    private static final Logger log = LoggerFactory.getLogger(Bot.class);
    private static final Mailer mailer = MailerBuilder
            .withSMTPServer(dotenv.get("SMTP_HOST"), Integer.parseInt(dotenv.get("SMTP_PORT")), dotenv.get("SMTP_USER"), dotenv.get("SMTP_PASS"))
            .buildMailer();
    private static final DatabaseHandler dbHandler = new DatabaseHandler(mailer);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Map<CmdData, Cmd> cmdMap;

    // ToDo: directly reply, then event.getHook().editOriginal() (instead of event.deferReply()) -- to get a custom "thinking" message

    public static void main(String[] args) {
        buildCmdMap();

        JDA jda = JDABuilder.createLight(dotenv.get("TOKEN"), EnumSet.noneOf(GatewayIntent.class)) // slash commands don't need any intents
                .addEventListeners(new Bot(), new DatabaseLoader(dbHandler), new ButtonListener())
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();
        startTime = Instant.now();

        // These commands might take a few minutes to be active after creation/update/delete
        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(getSlashCommands());

        // Send the new set of commands to discord, this will override any existing global commands with the new set provided here
        commands.queue();
        log.info("Registered slash commands");

        jda.getPresence().setActivity(Activity.customStatus("Observing citizens of " + dotenv.get("MAIN_SERVER")));
        jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);

        setUpVerification(jda);
    }

    private static void setUpVerification(JDA jda) {
        Guild guild = jda.getGuildById(dotenv.get("MAIN_SERVER_ID"));
        MessageChannel channel = guild.getTextChannelById(dotenv.get("VERIFICATION_CHANNEL_ID"));
        Role role = guild.getRoleById(dotenv.get("UNVERIFIED_ROLE_ID"));
        Verification verifSystem = new Verification(objectMapper, dbHandler, guild, channel, role);
        verifSystem.listenForWebhooks();
        jda.addEventListener(verifSystem);
    }

    private static void buildCmdMap() {
        cmdMap = Map.of(
                new CmdData("credit", "Add or subtract social credit from a user",
                        List.of(new CmdOption(USER, "user", "The user to add or subtract credit from", true),
                                new CmdOption(INTEGER, "amount", "Amount of credit to add or subtract", true),
                                new CmdOption(STRING, "reason", "Reason for adding or subtracting credit", false)),
                        true, false), new Credit(dbHandler),

                new CmdData("leaderboard", "View social credit rankings",
                        List.of(new CmdOption(INTEGER, "max", "Number of users (1 to 20) to display. If not provided, defaults to 10", false)),
                        true, true), new Leaderboard(dbHandler),

                new CmdData("profile", "View a user's social credit stats",
                        List.of(new CmdOption(USER, "user", "The user to view (if not given, defaults to yourself)", false)),
                        true, true), new Profile(dbHandler),

                new CmdData("s", "...",
                        List.of(new CmdOption(STRING, "co", "...", true),
                                new CmdOption(CHANNEL, "ch", "...", false)),
                        true, false), new Say(),

                new CmdData("cat", "Acquire a random cat picture",
                        List.of(),
                        true, true), new Cat(objectMapper),

                new CmdData("e", "...",
                        List.of(new CmdOption(STRING, "c", "...", true)),
                        true, false), new Eval(dotenv),

                new CmdData("t", "...",
                        List.of(new CmdOption(STRING, "p", "...", true)),
                        true, false), new Tanki(objectMapper),

                new CmdData("free_credits", "Get 9999 free credits!",
                        List.of(),
                        true, true), new FreeCredits(),

                new CmdData("rob", "Rob credits from someone else (there's a chance that they catch you in the act and rob you instead!)",
                        List.of(new CmdOption(USER, "user", "User to (try to) rob from", true)),
                        true, true), new Rob(dbHandler),

                new CmdData("daily", "Claim your free daily credits! Timer resets at 12 AM EST",
                        List.of(),
                        true, true), new Daily(dbHandler)
        );
        // hack to make cmdMap no longer immutable
        cmdMap = new HashMap<>(cmdMap);
        // add the 11th (& onwards) commands here, since Map::of only accepts at most 10 key-value pairs, apparently
        cmdMap.put(
                new CmdData("uptime", "Display how long I've been awake for",
                        List.of(),
                        true, true), new Uptime()
        );
        cmdMap.put(
                new CmdData("give", "Give some of your social credit to someone else",
                        List.of(new CmdOption(USER, "user", "The user to give credit to", true),
                                new CmdOption(INTEGER, "amount", "Amount of credit to give (must be positive)", true),
                                new CmdOption(STRING, "reason", "Reason for giving credit", false)),
                        true, true), new Give(dbHandler)
        );
        cmdMap.put(
                new CmdData("beg", "Beg for social credits in the chat",
                        List.of(new CmdOption(INTEGER, "amount", "Amount of credit to beg for (must be positive)", true)),
                        true, true), new Beg(dbHandler)
        );
        cmdMap.put(
                new CmdData("prune", "Prune (remove) a user from the database",
                        List.of(new CmdOption(USER, "user", "The user to prune", true)),
                        true, false), new Prune(dbHandler)
        );
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

    // Slash Commands
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // these commands should only work in guilds:
        if (event.getGuild() == null) {
            event.reply("My slash commands only work in servers!").setEphemeral(true).queue();
            return;
        }

        // ensure the commands will only work in main server or the test server
        String serverId = event.getGuild().getId();
        if (!serverId.equals(dotenv.get("MAIN_SERVER_ID")) && !serverId.equals(dotenv.get("TEST_SERVER_ID"))) {
            event.reply("NOOBY NOOB").queue();
            return;
        }

        String userId = event.getUser().getId();

        if (Boolean.parseBoolean(dotenv.get("ON_MAINTENANCE")) && !userId.equals(dotenv.get("DEV_ID"))) {
            event.reply("I'm currently on maintenance mode; I can't respond to anyone except the developer!").setEphemeral(true).queue();
            return;
        }

        boolean found = false;
        for (Map.Entry<CmdData, Cmd> entry : cmdMap.entrySet()) {
            CmdData cmdData = entry.getKey();
            Cmd cmd = entry.getValue();

            if (event.getName().equals(cmdData.name())) {
                found = true;

                // ensure only owner and dev can use the bot, except for whitelisted commands (which can be used by anyone)
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
