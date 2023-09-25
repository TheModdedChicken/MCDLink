package org.loganshaw.mcdlink.util;

import org.bukkit.Bukkit;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class TempPlayerLink {
    public long discord_id;
    public MinecraftUsername mc_username;
    public String id = getAlphaNumericString(8);
    public Date created = new Date();

    public TempPlayerLink (long discord_id, MinecraftUsername mc_username) throws NullPointerException {
        this.discord_id = discord_id;
        this.mc_username = mc_username;
    }

    static String getAlphaNumericString(int n)
    {
        String AlphaNumericString =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }

        return sb.toString();
    }
}
