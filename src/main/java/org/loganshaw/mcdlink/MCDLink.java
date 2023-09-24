package org.loganshaw.mcdlink;

import java.util.HashMap;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.loganshaw.mcdlink.util.DiscordManager;

public class MCDLink extends JavaPlugin implements Listener {

    public DiscordManager client;
    public MCDLink plugin;
    public ConsoleCommandSender console;
    public FileConfiguration config;

    @Override
    public void onEnable() {
        saveResource("config.yml", false);

        plugin = this;
        config = getConfig();
        console = Bukkit.getServer().getConsoleSender();

        String bot_token = config.getString("bot_token");
        long guild_id = config.getLong("guild_id");
        if (bot_token != null) {
            client = new DiscordManager(bot_token, guild_id, plugin);

            Bukkit.getPluginManager().registerEvents(this, this);
        }
        else console.sendMessage("Invalid Discord bot token");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
    }

}