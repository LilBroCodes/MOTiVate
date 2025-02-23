package org.lilbrocodes.motivate;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.lilbrocodes.motivate.common.InspectionResult;
import org.lilbrocodes.motivate.common.MOTD;
import org.lilbrocodes.motivate.implementation.Metrics;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public final class Motivate extends JavaPlugin {
    public MotivateUUIDStorage UUID_STORAGE;
    public boolean placeholderAPIEnabled = false;
    public FileConfiguration config;
    public final Logger logger = getLogger();

    @Override
    public void onEnable() {
        loadConfig();
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI != null && placeholderAPI.isEnabled() && config.getBoolean("store-player-data")) {
            this.placeholderAPIEnabled = true;
            getLogger().info(pluginFound("PlaceholderAPI"));
        } else {
            getLogger().info(pluginNotFound("PlaceholderAPI"));
        }

        getServer().getPluginManager().registerEvents(new MotivateEvents(this), this);
        this.UUID_STORAGE = new MotivateUUIDStorage(this.getDataFolder(), logger, config.getBoolean("store-player-data"));

        getLogger().info("MOTiVate enabled!");

        MotivateCommands commands = new MotivateCommands(this);
        Objects.requireNonNull(getCommand("motivate")).setExecutor(commands);
        Objects.requireNonNull(getCommand("motivate")).setTabCompleter(commands);

        Metrics metrics = new Metrics(this, 24271);
        metrics.addCustomChart(new Metrics.SimplePie("messages", () -> {
            InspectionResult inspection = validateMessages();
            if (inspection.success()) {
                return String.valueOf(config.getMapList("messages").size());
            } else {
                return "Broken config";
            }
        }));
        metrics.addCustomChart(new Metrics.SimplePie("version", () -> getDescription().getVersion()));
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

    public void writeConfig() throws IOException {
        config.save(Paths.get(getDataFolder().toString(), "config.yml").toString());
    }

    private InspectionResult validateMessages() {
        reloadConfigC();
        if (!config.contains("messages") || !(config.get("messages") instanceof List<?>)) {
            return new InspectionResult(false, "The 'messages' section is missing or is not a list.");
        }

        List<?> messages = config.getList("messages");

        for (int i = 0; i < Objects.requireNonNull(messages).size(); i++) {
            Object item = messages.get(i);

            Map<?, ?> message = null;
            if (!(item instanceof Map<?, ?>)) {
                return new InspectionResult(false, "Message #" + (i + 1) + " must be a map.");
            } else {
                message = (Map<?, ?>) item;
            }

            if (!message.containsKey("primary") || !(message.get("primary") instanceof String)) {
                return new InspectionResult(false, "Message #" + (i + 1) + " is missing the 'primary' field or it is not a string.");
            }

            if (!message.containsKey("secondary") || !(message.get("secondary") instanceof String)) {
                return new InspectionResult(false, "Message #" + (i + 1) + " is missing the 'secondary' field or it is not a string.");
            }

            if (message.containsKey("require-player-data") && !(message.get("require-player-data") instanceof Boolean)) {
                return new InspectionResult(false, "Message #" + (i + 1) + " has an invalid 'require-player-data' field (must be true or false).");
            }
        }

        return new InspectionResult(true, "Configuration is valid.");
    }

    public MOTD generateMOTD(String hostName) {
        InspectionResult result = validateMessages();
        if (!result.success()) {
            return new MOTD("FAIL", result.message(), true);
        }

        List<Map<?, ?>> messages = config.getMapList("messages");
        Random random = new Random();

        OfflinePlayer playerData = getOfflinePlayer(hostName);
        boolean hasPlayerData = playerData != null;

        List<Map<?, ?>> toRemove = new ArrayList<>();
        if (!hasPlayerData) {
            for (Map<?, ?> message : messages) {
                Object requiresPlayerData = message.get("requires-player-data");
                boolean requireData;
                if (requiresPlayerData instanceof Boolean) {
                    requireData = (Boolean) requiresPlayerData;
                } else if (requiresPlayerData instanceof String) {
                    requireData = Boolean.parseBoolean((String) requiresPlayerData);
                } else {
                    requireData = false;
                }
                if (requireData) {
                    toRemove.add(message);
                }
            }
        }
        messages.removeAll(toRemove);
        int randomId = random.nextInt(messages.size());

        if (messages.isEmpty()) {
            return new MOTD("§4ERROR §7§l| §r§fNo player data present, and no", "§fmessage that doesn't need it was found.", false);
        } else {
            Map<?, ?> message = messages.get(randomId);
            Object requiresPlayerData = message.get("requires-player-data");
            boolean requireData;
            if (requiresPlayerData instanceof Boolean) {
                requireData = (Boolean) requiresPlayerData;
            } else if (requiresPlayerData instanceof String) {
                requireData = Boolean.parseBoolean((String) requiresPlayerData);
            } else {
                requireData = false;
            }
            MOTD motd = new MOTD((String) message.get("primary"), (String) message.get("secondary"), requireData);

            if (placeholderAPIEnabled) {
                if (motd.requirePlayerData) {
                    if (getOfflinePlayer(hostName) == null) return new MOTD("§4ERROR §7§l| §r§fNo player data present, and no", "§fmessage that doesn't need it was found.", false);
                    motd.primary = parsePlaceholders(motd.primary, getOfflinePlayer(hostName));
                    motd.secondary = parsePlaceholders(motd.secondary, getOfflinePlayer(hostName));
                } else {
                    motd.primary = parsePlaceholders(motd.primary, null);
                    motd.secondary = parsePlaceholders(motd.secondary, null);
                }
            }

            return motd;
        }
    }

    public MOTD getMOTD(Map<?, ?> message, String hostName) {
        Object requiresPlayerData = message.get("requires-player-data");
        boolean requireData;
        if (requiresPlayerData instanceof Boolean) {
            requireData = (Boolean) requiresPlayerData;
        } else if (requiresPlayerData instanceof String) {
            requireData = Boolean.parseBoolean((String) requiresPlayerData);
        } else {
            requireData = false;
        }
        MOTD motd = new MOTD((String) message.get("primary"), (String) message.get("secondary"), requireData);

        if (placeholderAPIEnabled) {
            if (motd.requirePlayerData) {
                if (getOfflinePlayer(hostName) == null) return new MOTD("§4ERROR §7§l| §r§fNo player data present, and no", "§fmessage that doesn't need it was found.", false);
                motd.primary = parsePlaceholders(motd.primary, getOfflinePlayer(hostName));
                motd.secondary = parsePlaceholders(motd.secondary, getOfflinePlayer(hostName));
            } else {
                motd.primary = parsePlaceholders(motd.primary, null);
                motd.secondary = parsePlaceholders(motd.secondary, null);
            }
        }

        return motd;
    }

    public String parsePlaceholders(String string, OfflinePlayer player) {
        return PlaceholderAPI.setPlaceholders(player, string);
    }

    public OfflinePlayer getOfflinePlayer(String hostName) {
        UUID uuid = UUID_STORAGE.loadUUID(hostName);
        if (uuid != null) return Bukkit.getOfflinePlayer(uuid);
        else return null;
    }

    public static String rgb(int r, int g, int b) {
        return String.format("\u001b[38;2;%d;%d;%dm", r, g, b);
    }

    public static String pluginNotFound(String name) {
        return String.format("%s%s not found and/or disabled.%s", rgb(255, 0, 0), name, "\u001B[0m");
    }

    public static String pluginFound(String name) {
        return String.format("%s%s found and hooked.%s", rgb(0, 255, 0), name, "\u001B[0m");
    }

    public static String invalidUUID(String key) {
        return String.format("%sInvalid encrypted UUID for key: %s%s", rgb(255, 0, 0), key, "\u001B[0m");
    }

    public static String savedUUID(String name) {
        return String.format("%sSaved player data for new player %s%s", rgb(0, 255, 0), name, "\u001B[0m");
    }

    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage("§7[§6MOT§fi§eVate§8]§r " + message);
    }
}
