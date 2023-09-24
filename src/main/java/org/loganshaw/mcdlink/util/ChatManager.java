package org.loganshaw.mcdlink.util;

import org.bukkit.ChatColor;

public class ChatManager {
    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
