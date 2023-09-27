package org.loganshaw.mcdlink.util.managers;

import org.loganshaw.mcdlink.MCDLink;
import org.loganshaw.mcdlink.util.PUID;
import org.loganshaw.mcdlink.util.enums.PlatformType;
import org.loganshaw.mcdlink.util.TempPlayerLink;
import org.loganshaw.mcdlink.util.errors.TooManySimilarPlayerLinks;
import org.loganshaw.mcdlink.util.interfaces.ITempPlayerLinkCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PlayerLinkManager {
    private List<TempPlayerLink> tempPlayerLinks = new ArrayList<>();

    private final MCDLink plugin;

    public PlayerLinkManager (MCDLink plugin) {
        this.plugin = plugin;
    }

    /**
     * Adds a unique temporary player link to memory
     * @param discordID Temporary player link
     * @param puid PUID of user
     * @throws TooManySimilarPlayerLinks One or more temporary player links with identical identifiers
     */
    public TempPlayerLink createTempLink (long discordID, PUID puid, ITempPlayerLinkCallback timeout) throws TooManySimilarPlayerLinks {
        TempPlayerLink link = new TempPlayerLink(discordID, puid);

        Stream<TempPlayerLink> links = getTempLinks( l -> (
                l.discord_id == link.discord_id
                || (
                    Objects.equals(l.puid.uuid, link.puid.uuid)
                    && l.puid.platform == link.puid.platform
                )
                || Objects.equals(l.id, link.id)
        ));

        if (links.findAny().isPresent()) throw new TooManySimilarPlayerLinks();

        tempPlayerLinks.add(link);

        this.plugin.scheduleManager.Timeout(300000, () -> {
            TempPlayerLink tLink = this.getTempLinkByID(link.id);
            if (tLink != null) {
                try {
                    timeout.Callback(tLink);
                } catch (RuntimeException ignored) {}

                removeTempLink(tLink.id);
            }
        });

        return link;
    }

    public void removeTempLink (String id) {
        List<TempPlayerLink> newLinks = tempPlayerLinks.stream().filter(l -> !Objects.equals(l.id, id)).toList();
        tempPlayerLinks.clear();
        tempPlayerLinks.addAll(newLinks);
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
     * @param puid PUID of user
     * @return A temporary player link. Returns null if no link is found.
     */
    public TempPlayerLink getTempLinkByUsername (PUID puid) {
        try {
            return this.getUniqueTempLink(l ->
                    Objects.equals(l.puid.uuid, puid.uuid)
                    && l.puid.platform == puid.platform
            );
        } catch (TooManySimilarPlayerLinks e) {
            throw new RuntimeException(
                    "Found more than one temporary player link with the "
                    + (puid.platform == PlatformType.JAVA ? "Java" : "Bedrock")
                    + " username '" + this.plugin.minecraftManager.getUsernameFromPUID(puid) + "'."
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
