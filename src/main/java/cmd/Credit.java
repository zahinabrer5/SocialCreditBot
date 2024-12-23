package cmd;

import db.DatabaseHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import util.Util;

import java.awt.*;
import java.math.BigInteger;

public class Credit extends Cmd {
    private final DatabaseHandler dbHandler;

    public Credit(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        // the "user" option is required, so it doesn't need a null-check here
        User user = event.getOption("user").getAsUser();
        Member member = event.getOption("user").getAsMember();
        long amount = event.getOption("amount").getAsLong();
        credit(event, user, member, amount);
    }

    private void credit(SlashCommandInteractionEvent event, User user, Member member, long amount) {
        if (amount == 0) {
            event.reply("`amount` cannot be 0 (zero)!").setEphemeral(true).queue();
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
}
