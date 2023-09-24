package org.loganshaw.mcdlink;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.loganshaw.mcdlink.util.DiscordManager;
import org.loganshaw.mcdlink.util.MinecraftManager;

public class MCDLink extends JavaPlugin implements Listener {

    public DiscordManager discordManager;
    public MinecraftManager minecraftManager;
    public MCDLink plugin;
    public Logger logger;
    public ConsoleCommandSender console;
    public FileConfiguration config;

    @Override
    public void onEnable() {
        saveResource("config.yml", false);

        plugin = this;
        config = getConfig();
        logger = getLogger();
        console = Bukkit.getServer().getConsoleSender();

        try {
            discordManager = new DiscordManager(plugin);
        } catch (Exception err) {
            logger.severe(err.getMessage());
            return;
        };

        minecraftManager = new MinecraftManager(plugin);

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        minecraftManager.onPlayerJoin(event);
    }

}