package org.zahin.cmd;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Backhoe extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if (msg.getContentRaw().contains("backhoe")) {
            msg.reply("# BACKHOE BACKHOE").queue();
        }
    }
}
