package org.zahin.cmd;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Backhoe extends ListenerAdapter {
    private final JDA jda;

    public Backhoe(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if (msg.getAuthor().getId().equals(jda.getSelfUser().getId()))
            if (msg.getContentRaw().toLowerCase().contains("backhoe")) {
                msg.reply("# BACKHOE BACKHOE").queue();
            }
    }
}
