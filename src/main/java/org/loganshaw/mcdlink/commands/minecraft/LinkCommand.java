package org.loganshaw.mcdlink.commands.minecraft;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.loganshaw.mcdlink.MCDLink;
import org.loganshaw.mcdlink.util.PlayerLink;
import org.loganshaw.mcdlink.util.TempPlayerLink;
import org.loganshaw.mcdlink.util.enums.PlatformType;

import java.util.Objects;
import java.util.UUID;

public class LinkCommand implements CommandExecutor {
    MCDLink plugin;

    public LinkCommand (MCDLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Component.text("§cOnly players can execute this command!"));
            return true;
        }

        Player player = Bukkit.getPlayer(commandSender.getName());
        if (player == null) {
            commandSender.sendMessage(Component.text("§cSomething went wrong while trying to unlink your account :/"));
            return true;
        }

        TempPlayerLink tempLink = this.plugin.minecraftManager.playerLinkManager.getTempLinkByID(args[0]);
        if (tempLink == null || !Objects.equals(tempLink.puid.uuid, player.getUniqueId())) {
            commandSender.sendMessage(Component.text("§cInvalid link code"));
            return true;
        }

        long discordID = tempLink.discord_id;
        this.plugin.minecraftManager.playerLinkManager.removeTempLink(tempLink.id);

        try {
            PlayerLink playerLink = this.plugin.databaseManager.getPlayerLinkFromDiscordID(discordID);

            // Retain current links
            UUID javaUUID = playerLink != null && tempLink.puid.platform != PlatformType.JAVA
                    ? playerLink.javaUUID
                    : tempLink.puid.platform == PlatformType.JAVA
                        ? tempLink.puid.uuid
                        : null;
            UUID bedrockUUID = playerLink != null && tempLink.puid.platform != PlatformType.BEDROCK
                    ? playerLink.bedrockUUID
                    : tempLink.puid.platform == PlatformType.BEDROCK
                        ? tempLink.puid.uuid
                        : null;

            this.plugin.databaseManager.setLink( new PlayerLink(discordID, javaUUID, bedrockUUID) );

            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(Component.text("§aYour account is now linked to Discord!"));
        } catch (RuntimeException err) {
            commandSender.sendMessage(Component.text("§cSomething went wrong while trying to link your account :/"));
            this.plugin.logger.severe(err.toString());
            return false;
        }

        return true;
    }
}
