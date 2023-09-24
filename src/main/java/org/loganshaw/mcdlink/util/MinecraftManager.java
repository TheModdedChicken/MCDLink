package org.loganshaw.mcdlink.util;

import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.loganshaw.mcdlink.MCDLink;
import org.loganshaw.mcdlink.commands.minecraft.LinkCommand;
import org.loganshaw.mcdlink.commands.minecraft.UnlinkCommand;

public class MinecraftManager {
    public MinecraftManager (MCDLink plugin) {
        plugin.getCommand("mcd-link").setExecutor(new LinkCommand(plugin));
        plugin.getCommand("mcd-unlink").setExecutor(new UnlinkCommand(plugin));
    }

    public void onPlayerJoin (PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.ADVENTURE);
        player.sendMessage(Component.text("Hi, §e" + player.getName() + "§r. You will be §cun-whitelisted§r shortly if you do not run §b/mcd-link"));
    }
}
