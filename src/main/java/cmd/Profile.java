package cmd;

import db.DatabaseHandler;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Profile extends Cmd {
    private final DatabaseHandler dbHandler;

    public Profile(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        User user = event.getOption("user").getAsUser();
        Member member = event.getOption("user").getAsMember();
        profile(event, user, member);
    }

    private void profile(SlashCommandInteractionEvent event, User user, Member member) {
        event.reply("This command is a WIP c:").setEphemeral(true).queue();
    }
}
