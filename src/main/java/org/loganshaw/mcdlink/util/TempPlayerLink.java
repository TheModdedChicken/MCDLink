package org.loganshaw.mcdlink.util;

import java.util.Date;

public class TempPlayerLink {
    public long discord_id;
    public PUID puid;
    public String id = getAlphaNumericString(8);
    public Date created = new Date();

    public TempPlayerLink (long discord_id, PUID puid) throws NullPointerException {
        this.discord_id = discord_id;
        this.puid = puid;
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
