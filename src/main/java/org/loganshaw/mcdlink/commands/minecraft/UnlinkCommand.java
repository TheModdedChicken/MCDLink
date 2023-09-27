package org.loganshaw.mcdlink.commands.minecraft;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.loganshaw.mcdlink.MCDLink;
import org.loganshaw.mcdlink.util.PUID;
import org.loganshaw.mcdlink.util.PlayerLink;
import org.loganshaw.mcdlink.util.enums.PlatformType;

import java.util.UUID;

public class UnlinkCommand implements CommandExecutor {
    MCDLink plugin;

    public UnlinkCommand (MCDLink plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Component.text("§cOnly players can execute this command!"));
            return true;
        }

        Player player = Bukkit.getPlayer(commandSender.getName());
        if (player == null) {
            commandSender.sendMessage(Component.text("§cSomething went wrong while trying to unlink your account :/"));
            return false;
        }

        UUID uuid = player.getUniqueId();
        PlatformType platform = this.plugin.floodgate.isFloodgateId(uuid) ? PlatformType.BEDROCK : PlatformType.JAVA;
        PUID puid = new PUID(uuid, platform);

        PlayerLink playerLink = this.plugin.databaseManager.getPlayerLinkFromPUID(puid);
        if (playerLink == null) {
            commandSender.sendMessage(Component.text("§cYou do not have an account linked."));
            return false;
        }

        UUID javaUUID = puid.platform != PlatformType.JAVA
                ? playerLink.javaUUID
                : null;
        UUID bedrockUUID = puid.platform != PlatformType.BEDROCK
                ? playerLink.bedrockUUID
                : null;

        this.plugin.databaseManager.setLink(new PlayerLink(playerLink.discordID, javaUUID, bedrockUUID));

        commandSender.sendMessage(Component.text("§cUnlinked account. You'll be kicked in 30 seconds"));
        player.setGameMode(GameMode.ADVENTURE);
        this.plugin.minecraftManager.setPlayerWhitelist(player.getUniqueId(), false);
        this.plugin.scheduleManager.Timeout(30000, () -> {
            PlayerLink timeoutPlayerLink = this.plugin.databaseManager.getPlayerLinkFromPUID(puid);
            if (timeoutPlayerLink == null) this.plugin.minecraftManager.kickPlayer(player.getUniqueId());
        });

        return true;
    }
}
