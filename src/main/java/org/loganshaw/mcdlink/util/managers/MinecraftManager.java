package org.loganshaw.mcdlink.util.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.loganshaw.mcdlink.MCDLink;
import org.loganshaw.mcdlink.commands.minecraft.LinkCommand;
import org.loganshaw.mcdlink.commands.minecraft.UnlinkCommand;
import org.loganshaw.mcdlink.util.MinecraftUsername;
import org.loganshaw.mcdlink.util.TempPlayerLink;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class MinecraftManager {
    public final PlayerLinkManager playerLinkManager;
    MCDLink plugin;
    Server server;
    Logger logger;
    ConsoleCommandSender console;

    public MinecraftManager (MCDLink plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.logger = plugin.logger;
        this.console = plugin.console;
        this.playerLinkManager = new PlayerLinkManager(plugin);

        plugin.getCommand("mcd-link").setExecutor(new LinkCommand(plugin));
        plugin.getCommand("mcd-unlink").setExecutor(new UnlinkCommand(plugin));
    }

    public void onPlayerJoin (PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String username = player.getName();

        boolean isTempPlayer = this.playerLinkManager.getTempLinkByUsername(new MinecraftUsername(username)) != null;

        // Change to include current link code and Discord account name
        if (isTempPlayer) {
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage(Component.text("Hi, §e" + username + "§r. You will be §cun-whitelisted§r shortly if you do not run §b/mcd-link"));
        }
    }

    public void linkJavaUser () {

    }

    public String addTempPlayerLink (long discord_id, MinecraftUsername username) throws Exception {
        switch (username.getType()) {
            case JAVA -> {
                setPlayerWhitelist(username, true);
                // if (!playerExists) throw new Exception("Player '" + username.getUsername() + "' doesn't exist");

                // Create temporary player link & Remove player link after 5 minutes
                TempPlayerLink tpl = this.playerLinkManager.createTempLink(discord_id, username, (link) -> {
                    // Change to check if user was verified
                    setPlayerWhitelist(username, false);
                    Player player = Bukkit.getPlayer(username.getUsername());
                    if (player != null) player.kick();

                    logger.info("Un-whitelisted '" + player.getName() + "' with Discord ID: " + link.discord_id);
                });

                logger.info("Temporarily whitelisted '" + username.getUsername() + "' with Discord ID: " + tpl.discord_id);

                return tpl.id;
            }

            case BEDROCK -> {
                logger.info("Got to bedrock temporary player link with " + username.getUsername());
                return "something";
            }
        }

        return "something";
    }

    public void removeTempPlayerLink (String id) {
        TempPlayerLink link = this.playerLinkManager.getTempLinkByID(id);
        this.playerLinkManager.removeTempLink(link);
    }

    public void setPlayerWhitelist (MinecraftUsername username, boolean add) {
        switch (username.getType()) {
            case JAVA -> {
                String mc_username = username.getUsername();
                OfflinePlayer player = Bukkit.getOfflinePlayer(mc_username);

                BukkitTask task = Bukkit.getScheduler().runTask(plugin, () -> {
                    player.setWhitelisted(add);
                    Player onlinePlayer = Bukkit.getPlayer(mc_username);
                    if (onlinePlayer != null) onlinePlayer.kick();
                });
            }

            case BEDROCK -> logger.info("Got to bedrock whitelist with " + username.getUsername());
        }
    }
}
