package org.zahin.cmd;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.zahin.db.DatabaseHandler;

public class Daily extends Cmd {
    private final DatabaseHandler dbHandler;
    private final Dotenv dotenv;

    public Daily(DatabaseHandler dbHandler, Dotenv dotenv) {
        this.dbHandler = dbHandler;
        this.dotenv = dotenv;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        daily(event);
    }

    private void daily(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        //
    }
}
