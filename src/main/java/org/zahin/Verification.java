package org.zahin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.zahin.db.DatabaseHandler;

public class Verification extends ListenerAdapter {
    private final ObjectMapper objectMapper;
    private final DatabaseHandler dbHandler;
    private final Guild guild;

    public Verification(ObjectMapper objectMapper, DatabaseHandler dbHandler, Guild guild) {
        this.objectMapper = objectMapper;
        this.dbHandler = dbHandler;
        this.guild = guild;
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
}
