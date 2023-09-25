package org.loganshaw.mcdlink.util;

public class MinecraftUsername {
    private final MinecraftUsernameType type;
    private final String username;

    public MinecraftUsername (String mc_username) {
        this.type = mc_username.startsWith(".") ? MinecraftUsernameType.BEDROCK : MinecraftUsernameType.JAVA;
        this.username = mc_username.startsWith(".") ? mc_username.replaceFirst(".", "") : mc_username;
    }

    public MinecraftUsernameType getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }
}
