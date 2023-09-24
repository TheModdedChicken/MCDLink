package org.loganshaw.mcdlink.commands.minecraft;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.loganshaw.mcdlink.MCDLink;

public class LinkCommand implements CommandExecutor {
    MCDLink plugin;

    public LinkCommand (MCDLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        return false;
    }
}
