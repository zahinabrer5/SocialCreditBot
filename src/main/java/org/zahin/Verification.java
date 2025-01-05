package org.zahin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.zahin.db.DatabaseHandler;

public class Verification extends ListenerAdapter {
    private final ObjectMapper objectMapper;
    private final DatabaseHandler dbHandler;
    private final Guild guild;
    private final MessageChannel channel;
    private final Role role;

    public Verification(ObjectMapper objectMapper, DatabaseHandler dbHandler, Guild guild, MessageChannel channel, Role role) {
        this.objectMapper = objectMapper;
        this.dbHandler = dbHandler;
        this.guild = guild;
        this.channel = channel;
        this.role = role;
    }

    public void listenForWebhooks() {
        // Listen on http://localhost:7000 for webhooks from Apps Script
        Javalin app = Javalin.create().start("localhost", 7000);
        app.post("/webhook", ctx -> {
            String webhookData = ctx.body();
            JsonNode json = objectMapper.readTree(webhookData);
            verifyUser(json.get("discordUser").asText(), json.get("schoolEmail").asText());

            ctx.status(200).result("Webhook Received");
        });
    }

    public void verifyUser(String discordUser, String schoolEmail) {
        if (discordUser.isBlank() || schoolEmail.isBlank())
            return;

        // check if user with given username exists in the server
        // bots have actual tags, so they can't be verified
        Member member = guild.getMemberByTag(discordUser, "0000");
        if (member == null)
            return;

        if (!schoolEmail.endsWith("uottawa.ca") && !schoolEmail.endsWith("cmail.carleton.ca"))
            return;

        dbHandler.saveVerifCode(member.getId(), schoolEmail);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // check that we're in the correct server
        if (!event.getGuild().equals(guild))
            return;

        // check that the message is in the #verification channel
        if (!event.getMessage().getChannelId().equals(channel.getId()))
            return;

        // exit if user doesn't have @Unverified role
        Member member = event.getMember();
        if (!member.getRoles().contains(role))
            return;

        String givenCode = event.getMessage().getContentStripped();
        if (dbHandler.matchVerifCode(member.getId(), givenCode)) {
            guild.removeRoleFromMember(member, role).queue();
        } else {
            event.getMessage().reply("""
                    Something went wrong verifying your account! Make sure that the code you entered is correct and that
                    your message doesn't contain any formatting hints, then try again. If that doesn't work, please try
                    again in a few minutes. If the problem persists, contact an administrator by DM.
                    """).queue();
        }
    }
}
