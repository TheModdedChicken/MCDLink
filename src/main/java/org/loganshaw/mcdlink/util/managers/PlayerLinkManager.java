package org.loganshaw.mcdlink.util.managers;

import org.loganshaw.mcdlink.MCDLink;
import org.loganshaw.mcdlink.util.MinecraftUsername;
import org.loganshaw.mcdlink.util.MinecraftUsernameType;
import org.loganshaw.mcdlink.util.TempPlayerLink;
import org.loganshaw.mcdlink.util.errors.TooManySimilarPlayerLinks;
import org.loganshaw.mcdlink.util.interfaces.ITempPlayerLinkCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PlayerLinkManager {
    private final List<TempPlayerLink> tempPlayerLinks = new ArrayList<>();

    private final MCDLink plugin;

    public PlayerLinkManager (MCDLink plugin) {
        this.plugin = plugin;
    }

    /**
     * Adds a unique temporary player link to memory
     * @param discordID Temporary player link
     * @param username Minecraft username
     * @throws TooManySimilarPlayerLinks One or more temporary player links with identical identifiers
     */
    public TempPlayerLink createTempLink (long discordID, MinecraftUsername username, ITempPlayerLinkCallback timeout) throws TooManySimilarPlayerLinks {
        TempPlayerLink link = new TempPlayerLink(discordID, username);

        Stream<TempPlayerLink> links = getTempLinks( l -> (
                l.discord_id == link.discord_id
                || (
                    Objects.equals(l.mc_username.getUsername(), link.mc_username.getUsername())
                    && l.mc_username.getType() == link.mc_username.getType()
                )
                || Objects.equals(l.id, link.id)
        ));

        if (links.findAny().isPresent()) throw new TooManySimilarPlayerLinks();

        tempPlayerLinks.add(link);

        this.plugin.scheduleManager.Timeout(20000, () -> {
            if (this.getTempLinkByID(link.id) != null) timeout.Callback(link);
            removeTempLink(link);
        });

        return link;
    }

    public boolean removeTempLink (TempPlayerLink link) {
        // TO-DO: Fix
        return tempPlayerLinks.remove(link);
    }

    /**
     * Gets a temporary player link using a Discord ID
     * @param discord_id Discord user ID
     * @return A temporary player link. Returns null if no link is found.
     */
    public TempPlayerLink getTempLinkByDiscordID (long discord_id) {
        try {
            return this.getUniqueTempLink(l -> l.discord_id == discord_id);
        } catch (TooManySimilarPlayerLinks e) {
            throw new RuntimeException("Found more than one temporary player link with the Discord ID '" + discord_id + "'.");
        }
    }

    /**
     * Gets a temporary player link using a Minecraft username
     * @param username Minecraft username
     * @return A temporary player link. Returns null if no link is found.
     */
    public TempPlayerLink getTempLinkByUsername (MinecraftUsername username) {
        try {
            return this.getUniqueTempLink(l ->
                    Objects.equals(l.mc_username.getUsername(), username.getUsername())
                    && l.mc_username.getType() == username.getType()
            );
        } catch (TooManySimilarPlayerLinks e) {
            throw new RuntimeException(
                    "Found more than one temporary player link with the "
                    + (username.getType() == MinecraftUsernameType.JAVA ? "Java" : "Bedrock")
                    + " username '" + username.getUsername() + "'."
            );
        }
    }

    /**
     * Gets a temporary player link using its ID
     * @param id Temporary player link ID
     * @return A temporary player link. Returns null if no link is found.
     */
    public TempPlayerLink getTempLinkByID (String id) {
        try {
            return this.getUniqueTempLink(l -> Objects.equals(l.id, id));
        } catch (TooManySimilarPlayerLinks e) {
            throw new RuntimeException("Found more than one temporary player link with the ID '" + id + "'.");
        }
    }

    /**
     * Gets a temporary player link using a predicate
     * @param predicate A non-interfering, stateless predicate to apply to each element to determine if it should be included
     * @return A temporary player link. Returns null if no link is found.
     * @throws TooManySimilarPlayerLinks More than one temporary player link with identical identifiers
     */
    private TempPlayerLink getUniqueTempLink (Predicate<TempPlayerLink> predicate) throws TooManySimilarPlayerLinks {
        long count = tempPlayerLinks.stream().filter(predicate).count();

        if (count > 1) throw new TooManySimilarPlayerLinks();
        else if (count < 1) return null;

        return tempPlayerLinks.stream().filter(predicate).toList().get(0);
    }

    /**
     * Gets all temporary player links that match the predicate specified
     * @param predicate A non-interfering, stateless predicate to apply to each element to determine if it should be included
     * @return A stream of temporary player links
     */
    private Stream<TempPlayerLink> getTempLinks (Predicate<TempPlayerLink> predicate) {
        return tempPlayerLinks.stream().filter(predicate);
    }
}
