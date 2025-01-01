package org.zahin.cmd;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.zahin.db.DatabaseHandler;
import org.zahin.util.CustomEmbed;
import org.zahin.util.Util;

import java.awt.*;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Beg extends Cmd {
    private static DatabaseHandler dbHandler = null;
    private final Dotenv dotenv;
    private static final Map<String, SlashCommandInteractionEvent> map = new HashMap<>();
    private final Random rand;

    public Beg(DatabaseHandler dbHandler, Dotenv dotenv, Random rand) {
        Beg.dbHandler = dbHandler;
        this.dotenv = dotenv;
        this.rand = rand;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        long amount = event.getOption("amount").getAsLong();
        beg(event, BigInteger.valueOf(amount));
    }

    private void beg(SlashCommandInteractionEvent event, BigInteger amount) {
        if (amount.compareTo(BigInteger.ZERO) <= 0) {
            event.reply("`amount` must be positive!").setEphemeral(true).queue();
            return;
        }

        CustomEmbed embed = new CustomEmbed(dotenv);
        embed.setTitle(event.getUser().getName() + " is begging for " + amount + " social credit...");
        embed.setDescription("Press the \"Accept\" button to help <@" + event.getUser().getId() + "> out");
        embed.setThumbnail("https://i.imgur.com/8Ua4Lc8.png");
        embed.setColor(Color.WHITE);

        String nonce = "accept-" + generateNonce();

        event.getMessageChannel().sendMessageEmbeds(embed.build()).addActionRow(
                Button.primary(nonce, "Accept")
        ).setNonce(nonce).queue();

        map.put(nonce, event);

        event.reply("Beg request sent successfully").setEphemeral(true).queue();
    }

    private String generateNonce() {
        String nonce = Util.randAlphaNum(10, rand);
        while (map.containsKey(nonce))
            nonce = Util.randAlphaNum(10, rand);
        return nonce;
    }

    public static void resolveDonation(ButtonInteractionEvent buttonEvent) {
        if (dbHandler == null) return;

        String nonce = buttonEvent.getComponentId();
        for (Map.Entry<String, SlashCommandInteractionEvent> entry : map.entrySet()) {
            if (nonce.equals(entry.getKey())) {
                SlashCommandInteractionEvent slashEvent = entry.getValue();
                String giverId = buttonEvent.getUser().getId();
                String receiverId = slashEvent.getUser().getId();
                if (giverId.equals(receiverId)) return;
                BigInteger amount = BigInteger.valueOf(slashEvent.getOption("amount").getAsLong());

                dbHandler.update(receiverId, amount);
                dbHandler.update(giverId, amount.negate());

                buttonEvent.reply(String.format("<@%s> just donated %d social credit to <@%s>!", giverId, amount, receiverId)).queue();
                map.remove(nonce);
            }
        }
    }
}
