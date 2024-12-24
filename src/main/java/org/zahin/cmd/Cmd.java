package org.zahin.cmd;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public abstract class Cmd {
    public abstract void run(SlashCommandInteractionEvent event);
}
