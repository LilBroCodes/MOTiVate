package org.lilbrocodes.motivate;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.lilbrocodes.motivate.common.MOTD;

import java.util.Objects;

public class MotivateEvents implements Listener {
    private final Motivate plugin;

    public MotivateEvents(Motivate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        MOTD motd = plugin.generateMOTD(event.getAddress().getHostName().replace(".", "_"));
        if (motd.primary.equals("FAIL") && motd.requirePlayerData) event.setMotd("§4ERROR §7§l| §r§f" + motd.secondary);
        else event.setMotd(motd.primary + "\n" + motd.secondary);
    }

    @EventHandler
    public void savePlayerUUID(PlayerJoinEvent event) {
        plugin.UUID_STORAGE.saveIfNotExists(Objects.requireNonNull(event.getPlayer().getAddress()).getHostName().replace(".", "_"), event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
}
