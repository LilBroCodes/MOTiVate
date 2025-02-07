package org.lilbrocodes.motivate;

import org.bukkit.configuration.file.YamlConfiguration;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;

public class MotivateUUIDStorage {

    private final File file;
    private final File keyFile;
    private final Logger logger;
    private final boolean enabled;
    private SecretKey secretKey;
    private final Map<String, UUID> uuidMap = new HashMap<>();

    public MotivateUUIDStorage(File dataFolder, Logger logger, boolean enabled) {
        this.file = new File(dataFolder, "players.yml");
        this.keyFile = new File(dataFolder, "encryption.key");
        this.logger = logger;
        this.enabled = enabled;

        loadOrGenerateKey();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        loadAllUUIDs();
    }

    /**
     * Encrypts and saves UUID using AES-256
     */
    public void saveUUID(String hostName, UUID uuid, String name) {
        try {
            String hashedIP = hashIP(hostName);
            String encryptedUUID = encrypt(uuid.toString());

            uuidMap.put(hashedIP, uuid);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set(hashedIP, encryptedUUID);
            config.save(file);
            logger.info(Motivate.savedUUID(name));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt and save UUID", e);
        }
    }

    /**
     * Loads and decrypts UUID from hashed IP
     */
    public UUID loadUUID(String hostName) {
        if (!enabled) {
            return null;
        }
        loadAllUUIDs();
        return uuidMap.get(hashIP(hostName));
    }

    private void loadAllUUIDs() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                String encryptedUUID = config.getString(key);
                if (encryptedUUID == null) continue;

                String decryptedUUID = decrypt(encryptedUUID);
                UUID uuid = UUID.fromString(decryptedUUID);
                uuidMap.put(key, uuid);
            } catch (Exception e) {
                this.logger.info(Motivate.invalidUUID(key));
            }
        }
    }

    public void saveIfNotExists(String key, UUID value, String name) {
        if (!enabled) return;
        if (!uuidMap.containsValue(value)) {
            saveUUID(key, value, name);
        }
    }

    /** ==================== 🔒 AES-256 ENCRYPTION ==================== */

    private void loadOrGenerateKey() {
        try {
            if (keyFile.exists()) {
                FileInputStream fis = new FileInputStream(keyFile);
                byte[] keyBytes = new byte[(int) keyFile.length()];
                fis.read(keyBytes);
                fis.close();
                this.secretKey = new SecretKeySpec(keyBytes, "AES");
            } else {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, new SecureRandom());
                this.secretKey = keyGen.generateKey();

                FileOutputStream fos = new FileOutputStream(keyFile);
                fos.write(secretKey.getEncoded());
                fos.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load or generate encryption key", e);
        }
    }

    private String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        return new String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8);
    }

    /** ==================== 🔑 SHA-256 IP HASHING ==================== */

    private String hashIP(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash IP", e);
        }
    }
}
