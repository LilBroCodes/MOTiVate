package org.lilbrocodes.motivate;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.lilbrocodes.motivate.common.MOTD;

import java.net.InetAddress;
import java.util.*;

public final class Motivate extends JavaPlugin {
    public final MotivateUUIDStorage UUID_STORAGE = new MotivateUUIDStorage(this.getDataFolder());

    public static boolean placeholderAPIEnabled = false;
    public FileConfiguration config;

    @Override
    public void onEnable() {
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI != null && placeholderAPI.isEnabled()) {
            placeholderAPIEnabled = true;
            getLogger().info("PlaceholderAPI detected and enabled.");
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholder features will be disabled.");
        }

        getServer().getPluginManager().registerEvents(new MotivateEvents(this), this);
        loadConfig();

        getLogger().info("MOTiVate enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MOTiVate disabled!");
    }

    public void loadConfig() {
        this.saveDefaultConfig();
        config = getConfig();
    }

    public void reloadConfigC() {
        this.reloadConfig();
        config = this.getConfig();
    }

    // TODO: \/ \/
    // I'm leaving this comment so i know what i need to do:
    //        - Make it so that player data required messages are skipped,
    //          only show the error if there is none with no data required
    //
    //        - Set up commands to reload & edit the config(s) ingame
    public MOTD generateMOTD(String hostName) {
        reloadConfigC();
        List<Map<?, ?>> messages = config.getMapList("messages");
        Random random = new Random();

        int randomId = random.nextInt(messages.size());

        Map<?, ?> message = messages.get(randomId);
        MOTD motd = new MOTD((String) message.get("primary"), (String) message.get("secondary"), (boolean) message.get("requires-player-data"));

        if (placeholderAPIEnabled) {
            if (motd.requirePlayerData && getOfflinePlayer(hostName) == null) {
                motd.primary = "§4ERROR §7§l| §r§fNo player data present, and no";
                motd.secondary = "§fmessage that doesn't need it was found.";
            } else {
                motd.primary = parsePlaceholders(motd.primary, getOfflinePlayer(hostName));
                motd.secondary = parsePlaceholders(motd.secondary, getOfflinePlayer(hostName));
            }
        }

        return motd;
    }

    public String parsePlaceholders(String string, OfflinePlayer player) {
        String step1 = PlaceholderAPI.setPlaceholders(player, string);
        return step1.replace("%player_name%", player.getName());
    }

    public OfflinePlayer getOfflinePlayer(String hostName) {
        UUID uuid = UUID_STORAGE.loadUUID(hostName);
        if (uuid != null) return Bukkit.getOfflinePlayer(uuid);
        else return null;
    }
}
