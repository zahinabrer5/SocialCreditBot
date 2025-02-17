package org.zahin.cmd;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.zahin.db.DatabaseHandler;

import java.util.Map;

public class ReplyOnTrigger extends ListenerAdapter {
    private final JDA jda;
    private final DatabaseHandler dbHandler;

    public ReplyOnTrigger(JDA jda, DatabaseHandler dbHandler) {
        this.jda = jda;
        this.dbHandler = dbHandler;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message msg = event.getMessage();

//        if (msg.getAuthor().getId().equals(jda.getSelfUser().getId())) {
//            return;
//        }

        if (msg.getAuthor().isBot()) {
            return;
        }

        for (Map.Entry<String, String> entry : dbHandler.getReplyTable().entrySet()) {
            String trigger = entry.getKey();
            String reply = entry.getValue();

            if (msg.getContentRaw().toLowerCase().contains(trigger)) {
                msg.reply(reply).queue();
            }
        }
    }
}
