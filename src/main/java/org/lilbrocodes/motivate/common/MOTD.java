package org.lilbrocodes.motivate.common;

public class MOTD {
    public String primary;
    public String secondary;
    public final boolean requirePlayerData;

    public MOTD(String primary, String secondary, boolean requirePlayerData) {
        this.primary = primary;
        this.secondary = secondary;
        this.requirePlayerData = requirePlayerData;
    }
}
