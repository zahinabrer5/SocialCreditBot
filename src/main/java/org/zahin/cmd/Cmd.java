package org.zahin.cmd;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Cmd {
    public void run(SlashCommandInteractionEvent event) {
        event.reply("Hello there!").setEphemeral(true).queue();
    }
}
