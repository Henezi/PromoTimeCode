package com.kvassuk.promotimecode.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kvassuk.promotimecode.PromoTimeCode;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final PromoTimeCode plugin;
    private final Gson gson;
    private final File playerDataFolder;
    private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();

    public DataManager(PromoTimeCode plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .create();
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
    }

    public void loadAll() {
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
            return;
        }

        File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        int loaded = 0;
        for (File file : files) {
            try {
                String fileName = file.getName().replace(".json", "");
                UUID uuid = UUID.fromString(fileName);
                PlayerData data = loadPlayerData(uuid);
                if (data != null) {
                    playerCache.put(uuid, data);
                    loaded++;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Некорректное имя файла: " + file.getName());
            }
        }

        plugin.getLogger().info("Загружено данных игроков: " + loaded);
    }

    public PlayerData loadPlayerData(UUID uuid) {
        File file = new File(playerDataFolder, uuid.toString() + ".json");
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, PlayerData.class);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось загрузить данные игрока " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerCache.get(uuid);
        if (data == null || !data.isDirty()) return;

        File file = new File(playerDataFolder, uuid.toString() + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
            data.markClean();
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить данные игрока " + uuid + ": " + e.getMessage());
        }
    }

    public void saveAllDirty() {
        for (UUID uuid : playerCache.keySet()) {
            savePlayerData(uuid);
        }
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerCache.computeIfAbsent(uuid, key -> {
            PlayerData existing = loadPlayerData(uuid);
            if (existing != null) {
                return existing;
            }
            return new PlayerData(uuid);
        });
    }

    public PlayerData getOrCreatePlayerData(Player player) {
        return getPlayerData(player);
    }

    public void markDirty(UUID uuid) {
        PlayerData data = playerCache.get(uuid);
        if (data != null) {
            data.setDirty(true);
        }
    }

    public boolean hasActiveCode(Player player) {
        PlayerData data = getPlayerData(player);
        return data.hasActiveCode();
    }

    public void clearPlayerData(UUID uuid) {
        PlayerData data = playerCache.remove(uuid);
        if (data != null) {
            File file = new File(playerDataFolder, uuid.toString() + ".json");
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public int getPlayerCount() {
        return playerCache.size();
    }

    public Map<UUID, PlayerData> getPlayerCache() {
        return playerCache;
    }
}