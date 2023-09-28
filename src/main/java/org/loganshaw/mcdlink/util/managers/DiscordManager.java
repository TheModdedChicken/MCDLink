package org.loganshaw.mcdlink.util.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.*;
import org.loganshaw.mcdlink.MCDLink;
import org.loganshaw.mcdlink.util.DiscordCommand;
import org.loganshaw.mcdlink.util.PUID;
import org.loganshaw.mcdlink.util.PlayerLink;
import org.loganshaw.mcdlink.util.enums.PlatformType;
import org.loganshaw.mcdlink.util.TempPlayerLink;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/*
Javacord Documentation: https://javacord.org/wiki/
 */

public class DiscordManager {
    DiscordApi api;
    MCDLink plugin;
    Logger logger;
    FileConfiguration config;

    long guild_id;

    public DiscordManager (MCDLink plugin) throws Exception {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = plugin.config;

        String bot_token = this.config.getString("bot_token");
        this.guild_id = this.config.getLong("guild_id");
        if (bot_token != null) {

            this.api = new DiscordApiBuilder()
                    .setToken(bot_token)
                    .setIntents(Intent.MESSAGE_CONTENT)
                    .login().join();

            registerCommands();

            // Listen for, and execute, Discord command calls
            api.addSlashCommandCreateListener(event -> {
                SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
                commands.get(slashCommandInteraction.getCommandName()).operation.Operation(event, this.plugin);
            });

        }
        else {
            String msg = "Invalid Discord bot token.";
            logger.severe(msg);
            throw new Exception(msg);
        };
    }

    /**
     * Registers new Discord commands
     */
    public void registerCommands () {
        this.logger.info("Registering Discord commands...");
        Set<SlashCommand> globalCommands = api.getGlobalSlashCommands().join();
        Set<String> ignore = new HashSet<>();

        // Check for already existing commands
        for (SlashCommand command : globalCommands) {
            List<String> names = command.getFullCommandNames();
            if (names.isEmpty()) continue;

            String name = names.get(0);
            if (
                commands.containsKey(name)
                && command.getDescription().contains("[v" + plugin.getPluginMeta().getVersion() + "]")
            ) {
                ignore.add(name);
                logger.info("Command '" + name + "' already exists. Ignoring.");
            }
        }

        // Register new commands
        for (Map.Entry<String, DiscordCommand> cmd : this.commands.entrySet()) {
            String name = cmd.getKey();
            if (!ignore.contains(name)) {
                DiscordCommand data = cmd.getValue();

                if (data.options != null) SlashCommand.with(name, Versionize(data.description), data.options).createGlobal(api).join();
                else if (data.option != null) SlashCommand.with(name, Versionize(data.description),
                        new SlashCommandOptionBuilder()
                                .setType(data.option.getType())
                                .setName(data.option.getName())
                                .setDescription(data.option.getDescription())
                                .setRequired(data.option.isRequired())
                ).createGlobal(api).join();
                else SlashCommand.with(name, Versionize(data.description)).createGlobal(api).join();

                logger.info("Registered Discord command '" + name + "'.");
            }
        }

        logger.info("Finished loading Discord commands!");
    }

    /**
     * Appends the plugin version to the end of a command description
     * @param description A command description.
     * @return A description with the appended plugin version.
     */
    public String Versionize (String description) {
        return description + " [v" + plugin.getPluginMeta().getVersion() + "]";
    }

    Map<String, DiscordCommand> commands = new HashMap<>() {{
        put("ping", new DiscordCommand(
                "Check bot status",
                (event, plugin) ->
                        event.getInteraction().createImmediateResponder()
                                .setContent("Pong!")
                                .setFlags(MessageFlag.EPHEMERAL)
                                .respond()
        ));

        put("link", new DiscordCommand(
                "Link Minecraft account",
                Arrays.asList(
                        SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "java", "Links a Java account",
                                List.of(
                                        SlashCommandOption.create(SlashCommandOptionType.STRING, "username", "A valid Java username", true)
                                )
                        ),
                        SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "bedrock", "Links a Bedrock/Xbox account",
                                List.of(
                                        SlashCommandOption.create(SlashCommandOptionType.STRING, "username", "A valid Bedrock/Xbox username", true)
                                )
                        )
                ),
                (event, plugin) -> {
                    SlashCommandInteraction interaction = event.getSlashCommandInteraction();
                    String subCommand = interaction.getFullCommandName().split(" ", 0)[1];

                    long user_id = interaction.getUser().getId();
                    PlatformType platformType = Objects.equals(subCommand, "java") ? PlatformType.JAVA : PlatformType.BEDROCK;
                    String username = interaction.getArgumentStringValueByName("username").orElse("");
                    UUID uuid = platformType == PlatformType.JAVA
                            ? plugin.server.getOfflinePlayer(username).getUniqueId()
                            : plugin.floodgate.getUuidFor(username).join();

                    PUID puid = new PUID(uuid, platformType);
                    String platform = puid.platform == PlatformType.JAVA ? "Java" : "Bedrock";

                    // Check if user is already trying to link an account
                    TempPlayerLink discordIDTempCheck = plugin.minecraftManager.playerLinkManager.getTempLinkByDiscordID(user_id);
                    PlayerLink puidCheck = plugin.databaseManager.getPlayerLinkFromPUID(puid);

                    if (discordIDTempCheck != null) {
                        interaction.createImmediateResponder()
                                .setContent("You're already trying to link `" + username + "` on " + platform + ".")
                                .setFlags(MessageFlag.EPHEMERAL)
                                .respond();
                    }
                    else if (puidCheck != null) {
                        if (puidCheck.discordID == user_id) {
                            interaction.createImmediateResponder()
                                    .setContent(
                                            "You already have `" + username + "` linked on " + platform + "."
                                            + "\nPlease use `/unlink` before trying to link a new account."
                                    )
                                    .setFlags(MessageFlag.EPHEMERAL)
                                    .respond();
                        } else {
                            interaction.createImmediateResponder()
                                    .setContent("You're already trying to link `" + username + "` on " + platform + ".")
                                    .setFlags(MessageFlag.EPHEMERAL)
                                    .respond();
                        }
                    }
                    else {
                        interaction.respondLater(true)
                                .thenAccept(interactionUpdater -> {
                                    try {
                                        String link_id = plugin.minecraftManager.addTempPlayerLink(user_id, puid);

                                        interactionUpdater
                                                .setContent("Please run `/mcd-link " + link_id + "` on your " + platform + " account (`" + username + "`)")
                                                .update();
                                    } catch (Exception err) {
                                        logger.info(err.toString());
                                        logger.info(Arrays.toString(err.getStackTrace()));

                                        interactionUpdater
                                                .setContent(err.getMessage())
                                                .update();
                                    }
                                });

                    }
                }
        ));

        put("unlink", new DiscordCommand(
                "Unlink Minecraft account",
                Arrays.asList(
                        SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "java", "Unlinks a Java account"),
                        SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "bedrock", "Unlinks a Bedrock/Xbox account")
                ),
                (event, plugin) -> {
                    SlashCommandInteraction interaction = event.getSlashCommandInteraction();
                    String subCommand = interaction.getFullCommandName().split(" ", 0)[1];

                    long user_id = interaction.getUser().getId();
                    PlatformType platformType = Objects.equals(subCommand, "java") ? PlatformType.JAVA : PlatformType.BEDROCK;
                    String platform = platformType == PlatformType.JAVA ? "Java" : "Bedrock";

                    PlayerLink playerLink = plugin.databaseManager.getPlayerLinkFromDiscordID(user_id);

                    if (playerLink == null) {
                        interaction.createImmediateResponder()
                                .setContent("You don't have any accounts linked.")
                                .setFlags(MessageFlag.EPHEMERAL)
                                .respond();
                    }
                    else if (
                            (playerLink.javaUUID != null && platformType == PlatformType.JAVA)
                            || (playerLink.bedrockUUID != null && platformType == PlatformType.BEDROCK)
                    ) {
                        try {
                            // Retain current links
                            UUID javaUUID = platformType != PlatformType.JAVA
                                    ? playerLink.javaUUID
                                    : null;
                            UUID bedrockUUID = platformType != PlatformType.BEDROCK
                                    ? playerLink.bedrockUUID
                                    : null;

                            plugin.databaseManager.setLink(new PlayerLink(playerLink.discordID, javaUUID, bedrockUUID));

                            UUID uuid = platformType == PlatformType.JAVA ? playerLink.javaUUID : playerLink.bedrockUUID;
                            plugin.minecraftManager.setPlayerWhitelist(uuid, false);
                            plugin.minecraftManager.kickPlayer(uuid);

                            interaction.createImmediateResponder()
                                    .setContent("Unlinked your " + platform + " account.")
                                    .setFlags(MessageFlag.EPHEMERAL)
                                    .respond();
                        } catch (RuntimeException err) {
                            interaction.createImmediateResponder()
                                    .setContent("Something went wrong while trying to unlink your " + platform + " account :/")
                                    .setFlags(MessageFlag.EPHEMERAL)
                                    .respond();
                        }
                    } else {
                        interaction.createImmediateResponder()
                                .setContent("You don't have any accounts linked on " + platform + ".")
                                .setFlags(MessageFlag.EPHEMERAL)
                                .respond();
                    }
                }
        ));

        put("whois", new DiscordCommand(
                "Check a user's linked Minecraft accounts",
                SlashCommandOption.create(SlashCommandOptionType.USER, "user", "User to check", true),
                (event, plugin) -> {
                    SlashCommandInteraction interaction = event.getSlashCommandInteraction();
                    User user = interaction.getArgumentUserValueByName("user").orElse(null);
                    if (user == null) interaction.createImmediateResponder()
                            .setContent("Couldn't find specified user.")
                            .setFlags(MessageFlag.EPHEMERAL)
                            .respond();

                    long userID = user.getId();

                    PlayerLink playerLink = plugin.databaseManager.getPlayerLinkFromDiscordID(userID);
                    if (playerLink == null) interaction.createImmediateResponder()
                            .setContent("<@" + userID + "> doesn't have any linked accounts.")
                            .setFlags(MessageFlag.EPHEMERAL)
                            .respond();

                    String javaUsername = playerLink.javaUUID != null
                            ? plugin.minecraftManager.getUsernameFromPUID(new PUID(playerLink.javaUUID, PlatformType.JAVA))
                            : "None";
                    String bedrockUsername = playerLink.bedrockUUID != null
                            ? plugin.minecraftManager.getUsernameFromPUID(new PUID(playerLink.bedrockUUID, PlatformType.BEDROCK))
                            : "None";

                    interaction.createImmediateResponder()
                            .addEmbed(new EmbedBuilder()
                                    .setTitle(user.getName())
                                    .addField("Java", javaUsername)
                                    .addField("Bedrock", bedrockUsername)
                                    .setFooter("ID: " + userID)
                                    .setColor(Color.decode("#5da4ff"))
                            )
                            .setFlags(MessageFlag.EPHEMERAL)
                            .respond();
                }
        ));
    }};
}
