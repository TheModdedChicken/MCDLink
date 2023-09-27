package org.loganshaw.mcdlink.util;

import org.loganshaw.mcdlink.util.enums.PlatformType;

import java.util.UUID;

// Platform User ID (A UUID with a platform indicator)
public class PUID {
    public final UUID uuid;
    public final PlatformType platform;

    public PUID (UUID uuid, PlatformType platform) {
        this.uuid = uuid;
        this.platform = platform;
    }
}
