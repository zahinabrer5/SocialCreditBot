package cmd;

import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class Say extends Cmd {
    @Override
    public void run(SlashCommandInteractionEvent event) {
        // content and channel are required so no null-check here
        String content = event.getOption("content").getAsString();
        OptionMapping channelOption = event.getOption("channel");
        Channel channel = event.getMessageChannel();
        if (channelOption != null)
            channel = channelOption.getAsChannel();
        say(event, content, channel);
    }

    private void say(SlashCommandInteractionEvent event, String content, Channel channel) {
        if (channel.getType() != ChannelType.TEXT) {
            event.reply("`channel` must be a text channel!").setEphemeral(true).queue();
            return;
        }

        event.getGuild().getTextChannelById(channel.getId()).sendMessage(content).queue();
        event.reply(String.format("Sent message successfully to <#%s>", channel.getId())).setEphemeral(true).queue();
    }
}
