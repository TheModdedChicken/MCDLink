package org.loganshaw.mcdlink.util.interfaces;

import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.loganshaw.mcdlink.MCDLink;

import java.util.Optional;

public interface IDiscordCommandOperation {
    void Operation (SlashCommandCreateEvent event, MCDLink plugin);
}
