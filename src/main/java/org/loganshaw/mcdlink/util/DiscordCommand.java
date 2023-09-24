package org.loganshaw.mcdlink.util;

import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.loganshaw.mcdlink.util.interfaces.IDiscordCommandOperation;

import java.util.List;

public class DiscordCommand {
    public IDiscordCommandOperation operation;
    public SlashCommandOptionBuilder option;
    public List<SlashCommandOption> options;
    public String description;

    /**
     * Stores a Discord command without options
     * @param description Command description
     * @param operation Function to execute when called
     */
    public DiscordCommand (String description, IDiscordCommandOperation operation) {
        this.description = description;
        this.operation = operation;
    }

    /**
     * Stores a Discord command with an option
     * @param description Command description
     * @param option Command option
     * @param operation Function to execute when called
     */
    public DiscordCommand (String description, SlashCommandOptionBuilder option, IDiscordCommandOperation operation) {
        this.description = description;
        this.operation = operation;
        this.option = option;
    }

    /**
     * Stores a Discord command with options
     * @param description Command description
     * @param options Command options
     * @param operation Function to execute when called
     */
    public DiscordCommand (String description, List<SlashCommandOption> options, IDiscordCommandOperation operation) {
        this.description = description;
        this.operation = operation;
        this.options = options;
    }
}
