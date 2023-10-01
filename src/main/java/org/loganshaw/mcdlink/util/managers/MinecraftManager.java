package org.loganshaw.mcdlink.util.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.javacord.api.entity.user.User;
import org.loganshaw.mcdlink.MCDLink;
import org.loganshaw.mcdlink.commands.minecraft.LinkCommand;
import org.loganshaw.mcdlink.commands.minecraft.UnlinkCommand;
import org.loganshaw.mcdlink.util.PUID;
import org.loganshaw.mcdlink.util.TempPlayerLink;
import org.loganshaw.mcdlink.util.enums.PlatformType;

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
        UUID uuid = player.getUniqueId();
        PlatformType platform = this.plugin.floodgate.isFloodgateId(uuid) ? PlatformType.BEDROCK : PlatformType.JAVA;

        TempPlayerLink tempPlayerLink = this.playerLinkManager.getTempLinkByPUID(new PUID(uuid, platform));
        boolean isTempPlayer = tempPlayerLink != null;

        if (isTempPlayer) {
            User discordUser = this.plugin.discordManager.api.getUserById(tempPlayerLink.discord_id).join();
            String discordName = discordUser.getName();

            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage(Component.text(
                    "Please run §b/mcd-link " + tempPlayerLink.id + "§r to link your Discord account (" + discordName + "), or you'll be §ckicked§r in §e5 minutes§r."
            ));
        }
    }

    public String addTempPlayerLink (long discord_id, PUID puid) throws Exception {
        String username = getUsernameFromPUID(puid);

        setPlayerWhitelist(puid.uuid, true);

        // Create temporary player link & Remove player link after 5 minutes
        TempPlayerLink tpl = this.playerLinkManager.createTempLink(discord_id, puid, (link) -> {
            // Change to check if user was verified
            setPlayerWhitelist(puid.uuid, false);
            kickPlayer(puid.uuid);

            try {
                Player player = this.plugin.server.getPlayer(puid.uuid);
                if (player != null) this.plugin.scheduleManager.SyncTask(player::kick);
            } catch (RuntimeException ignored) {}

            logger.info("Un-whitelisted '" + username + "' (" + puid.uuid + ") with Discord ID '" + link.discord_id + "'.");
        });

        logger.info("Temporarily whitelisted '" + username + "' (" + puid.uuid + ") with Discord ID '" + tpl.discord_id + "'. (" + tpl.id + ")");

        return tpl.id;
    }

    public void removeTempPlayerLink (String id) {
        TempPlayerLink link = this.playerLinkManager.getTempLinkByID(id);
        this.playerLinkManager.removeTempLink(link.id);
    }

    public String getUsernameFromPUID (PUID puid) {
        return puid.platform == PlatformType.JAVA
                ? plugin.server.getOfflinePlayer(puid.uuid).getName()
                : plugin.floodgate.getGamertagFor(getXUIDFromPUID(puid)).join();
    }

    public long getXUIDFromPUID (PUID puid) {
        String stringifiedXUID = puid.uuid.toString().split("-", 3)[2].replace("-", "");
        return Long.parseLong(stringifiedXUID, 16);
    }

    public void setPlayerWhitelist (UUID uuid, boolean add) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

        BukkitTask task = Bukkit.getScheduler().runTask(plugin, () -> {
            offlinePlayer.setWhitelisted(add);
        });
    }

    public void kickPlayer (UUID uuid) {
        BukkitTask task = Bukkit.getScheduler().runTask(plugin, () -> {
            Player onlinePlayer = this.plugin.server.getPlayer(uuid);
            if (onlinePlayer != null) onlinePlayer.kick();
        });
    }
}
