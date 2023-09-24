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
import org.loganshaw.mcdlink.util.TempPlayerLink;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class MinecraftManager {
    public Map<Long, TempPlayerLink> tempPlayerLinks = new HashMap<>();
    MCDLink plugin;
    Server server;
    Logger logger;
    ConsoleCommandSender console;
    File file;
    YamlConfiguration links;

    public MinecraftManager (MCDLink plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.logger = plugin.logger;
        this.console = plugin.console;

        plugin.getCommand("mcd-link").setExecutor(new LinkCommand(plugin));
        plugin.getCommand("mcd-unlink").setExecutor(new UnlinkCommand(plugin));
    }

    public void onPlayerJoin (PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        boolean isTempPlayer = tempPlayerLinks.entrySet().stream().anyMatch(
            tpl ->
            (Objects.equals(tpl.getValue().java_username, playerName))
            || Objects.equals(tpl.getValue().bedrock_username, playerName)
        );

        // Change to include current link code and Discord account name
        if (isTempPlayer) {
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage(Component.text("Hi, §e" + playerName + "§r. You will be §cun-whitelisted§r shortly if you do not run §b/mcd-link"));
        }
    }

    public void linkJavaUser () {

    }

    public String addTempPlayerLink (long discord_id, String mc_username) throws Exception {
        boolean playerExists = setPlayerWhitelist(mc_username, true);
        if (!playerExists) throw new Exception("Player '" + mc_username + "' doesn't exist");

        // Create temporary player link
        TempPlayerLink tpl = new TempPlayerLink(discord_id, mc_username);
        tempPlayerLinks.put(discord_id, tpl);

        // Remove player link after 5 minutes
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            new java.util.Timer().schedule( new java.util.TimerTask() {
                    @Override
                    public void run() {
                        // Change to check if user was verified
                        removeTempPlayerLink(discord_id);
                        Player player = Bukkit.getPlayer(mc_username);
                        if (player != null) player.kick();

                        logger.info("Un-whitelisted '" + player.getName() + "' with Discord ID: " + tpl.discord_id);
                    }
                },
                300000
            );
        });

        logger.info("Temporarily whitelisted '" + mc_username + "' with Discord ID: " + tpl.discord_id);

        return tpl.id;
    }

    public void removeTempPlayerLink (long discord_id) {
        TempPlayerLink tpl = tempPlayerLinks.get(discord_id);
        String username = tpl.java_username != null ? tpl.java_username : tpl.bedrock_username;

        setPlayerWhitelist(username, false);
        tempPlayerLinks.remove(tpl.discord_id);
    }

    public boolean setPlayerWhitelist (String mc_username, boolean add) {
        String whitelist_type = mc_username.startsWith(".") ? "bedrock" : "java";
        String username = mc_username.startsWith(".") ? mc_username.replaceFirst(".", "") : mc_username;

        OfflinePlayer player = this.getOfflinePlayer(username);

        if (player != null) {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, () -> {
                switch (whitelist_type) {
                    case "java" -> {
                        player.setWhitelisted(add);
                        Player onlinePlayer = Bukkit.getPlayer(username);
                        if (onlinePlayer != null) onlinePlayer.kick();
                    }
                    case "bedrock" -> logger.info("Got to bedrock whitelist with " + username);
                }
            });

            return true;
        } else return false;
    }

    public OfflinePlayer getOfflinePlayer(String name) {
        for(OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if(player.getName().equals(name)) return player;
        }
        return null;
    }
}
