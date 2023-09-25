package org.loganshaw.mcdlink.commands.minecraft;

import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.loganshaw.mcdlink.MCDLink;

public class LinkCommand implements CommandExecutor {
    MCDLink plugin;

    public LinkCommand (MCDLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        this.plugin.minecraftManager.removeTempPlayerLink(args[0]);

        Player player = commandSender.getServer().getPlayer(commandSender.getName());
        player.setGameMode(GameMode.SURVIVAL);
        player.sendMessage(Component.text("Your account is now linked to Discord!"));

        return false;
    }
}
