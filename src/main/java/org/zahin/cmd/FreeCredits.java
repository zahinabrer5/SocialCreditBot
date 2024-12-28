package org.zahin.cmd;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class FreeCredits extends Cmd {
    @Override
    public void run(SlashCommandInteractionEvent event) {
        event.reply("NOOB").queue();
    }
}
