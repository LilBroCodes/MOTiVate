package org.lilbrocodes.motivate;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class MotivateUUIDStorage {

    private final File file;
    private final Map<String, UUID> uuidMap = new HashMap<>();

    public MotivateUUIDStorage(File dataFolder) {
        this.file = new File(dataFolder, "players.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        loadAllUUIDs();
    }

    /**
     * Saves a player's UUID associated with their InetAddress to the file and updates the map.
     *
     * @param address The InetAddress key for the UUID.
     * @param uuid    The UUID to save.
     */
    public void saveUUID(String  hostName, UUID uuid) {
        uuidMap.put(hostName, uuid);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(hostName, uuid.toString());
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a UUID from the map based on the InetAddress.
     *
     * @param address The InetAddress key for the UUID.
     * @return The UUID, or null if not found.
     */
    public UUID loadUUID(String hostName) {
        loadAllUUIDs();
        return uuidMap.get(hostName);
    }

    /**
     * Loads all UUIDs from the file into the map.
     */
    private void loadAllUUIDs() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(config.getString(key));
                uuidMap.put(key, uuid);
            } catch (Exception e) {
                System.err.println("Invalid InetAddress or UUID key: " + key);
            }
        }
    }

    public void saveIfNotExists(String key, UUID value) {
        Collection<UUID> existing = this.uuidMap.values();
        if (!existing.contains(value)) {
            saveUUID(key, value);
        }
    }
}
