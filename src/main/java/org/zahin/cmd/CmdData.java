package org.zahin.cmd;

import java.util.List;

public record CmdData(String name, String description, List<CmdOption> options, boolean guildOnly, boolean whitelisted) {}
