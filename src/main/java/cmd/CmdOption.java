package cmd;

import net.dv8tion.jda.api.interactions.commands.OptionType;

public record CmdOption(OptionType optionType, String name, String description, boolean required) {}
