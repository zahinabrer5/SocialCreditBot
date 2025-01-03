package org.zahin.cmd;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.db.DatabaseHandler;

public class Prune extends Cmd {
    private final DatabaseHandler dbHandler;

    public Prune(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        User user = event.getOption("user").getAsUser();
        prune(event, user);
    }

    private void prune(SlashCommandInteractionEvent event, User user) {
        String userId = user.getId();
        dbHandler.removeUserRow(userId);
        event.reply(String.format("Successfully removed <@%s> from the database!", userId)).queue();
    }
}
