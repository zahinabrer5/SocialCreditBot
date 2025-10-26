package org.zahin.cmd;

import io.github.cdimascio.dotenv.Dotenv;
import jdk.jshell.JShell;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Eval extends Cmd {
    private final Dotenv dotenv;

    public Eval(Dotenv dotenv) {
        this.dotenv = dotenv;
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {
        if (!event.getUser().getId().equals(dotenv.get("DEV_ID")))
            return;

        String code = event.getOption("c").getAsString();

        eval(event, code);
    }

    private void eval(SlashCommandInteractionEvent event, String code) {
        // https://stackoverflow.com/a/52529029/21405641
        try (JShell js = JShell.create()) {
            js.onSnippetEvent(snip -> {
                if (snip.status() == jdk.jshell.Snippet.Status.VALID) {
                    event.reply(String.format("""
                            ```java
                            \u200B
                            ➜ %s```""", snip.value())).setEphemeral(true).queue();
                }
            });

            js.eval(js.sourceCodeAnalysis().analyzeCompletion(code).source());
        }
    }
}
