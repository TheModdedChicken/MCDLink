package org.loganshaw.mcdlink.util;

import com.avaje.ebean.validation.NotNull;

import java.util.Date;
import java.util.UUID;

public class PlayerLink {
    @NotNull public final long discordID;
    public final UUID javaUUID;
    public final UUID bedrockUUID;
    public final Date created;

    public PlayerLink (long discordID, UUID javaUUID, UUID bedrockUUID) {
        this.discordID = discordID;
        this.javaUUID = javaUUID;
        this.bedrockUUID = bedrockUUID;
        this.created = null;
    }

    public PlayerLink (long discordID, UUID javaUUID, UUID bedrockUUID, Date created) {
        this.discordID = discordID;
        this.javaUUID = javaUUID;
        this.bedrockUUID = bedrockUUID;
        this.created = created;
    }
}
