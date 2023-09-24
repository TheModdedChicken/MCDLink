package org.loganshaw.mcdlink.util.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.interaction.*;
import org.loganshaw.mcdlink.MCDLink;
import org.loganshaw.mcdlink.util.DiscordCommand;
import org.loganshaw.mcdlink.util.TempPlayerLink;

import java.util.*;
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
                commands.get(slashCommandInteraction.getCommandName()).operation.Operation(event);
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
                else if (data.option != null) SlashCommand.with(name, Versionize(data.description), data.option).createGlobal(api).join();
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
                event ->
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
                event -> {
                    SlashCommandInteraction interaction = event.getSlashCommandInteraction();
                    String subCommand = interaction.getFullCommandName().split(" ", 0)[1];

                    long user_id = interaction.getUser().getId();
                    String mc_username = interaction.getArgumentStringValueByName("username").orElse("");

                    // Check if user is already trying to link an account
                    if (plugin.minecraftManager.tempPlayerLinks.containsKey(user_id)) {
                        TempPlayerLink tpl = plugin.minecraftManager.tempPlayerLinks.get(user_id);
                        boolean isJava = tpl.java_username != null;
                        String username = isJava ? tpl.java_username : tpl.bedrock_username;
                        String platform = isJava ? "Java" : "Bedrock";

                        interaction.createImmediateResponder()
                                .setContent("You're already trying to link `" + username + "` on " + platform + ".")
                                .setFlags(MessageFlag.EPHEMERAL)
                                .respond();
                    }
                    else {
                        interaction.respondLater(true)
                                .thenAccept(interactionUpdater -> {
                                    try {
                                        String link_id = plugin.minecraftManager.addTempPlayerLink(
                                                user_id,
                                                Objects.equals(subCommand, "java") ? mc_username : ("." + mc_username)
                                        );

                                        interactionUpdater
                                                .setContent("Please run `/mcd-link " + link_id + "` on your Java account (`" + mc_username + "`)")
                                                .update();
                                    } catch (Exception err) {
                                        interactionUpdater
                                                .setContent(err.getMessage())
                                                .update();
                                    }
                                });

                    }
                }
        ));
    }};
}
