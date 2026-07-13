package com.yourname.promotimecode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final File dataFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public DataManager() {
        dataFile = new File(PromoTimeCode.getInstance().getDataFolder(), "playerdata.json");
        load();
    }

    private String encode(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String base64) {
        byte[] decoded = Base64.getDecoder().decode(base64);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public void load() {
        if (!dataFile.exists()) {
            playerDataMap = new HashMap<>();
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String json = decode(sb.toString());
            Type type = new TypeToken<Map<UUID, PlayerData>>(){}.getType();
            playerDataMap = gson.fromJson(json, type);
            if (playerDataMap == null) playerDataMap = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
            playerDataMap = new HashMap<>();
        }
    }

    public void saveAll() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            String json = gson.toJson(playerDataMap);
            writer.write(encode(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PlayerData getPlayerData(Player p) {
        return playerDataMap.computeIfAbsent(p.getUniqueId(), k -> new PlayerData());
    }

    public void setPlayerData(Player p, PlayerData data) {
        playerDataMap.put(p.getUniqueId(), data);
        saveAll();
    }

    public void clearPlayerData(Player p) {
        playerDataMap.remove(p.getUniqueId());
        saveAll();
    }

    public static class PlayerData {
        public String activeCode = null;
        public int playedSeconds = 0;
        public boolean redeemed = false;

        public PlayerData() {
            this.activeCode = null;
            this.playedSeconds = 0;
            this.redeemed = false;
        }
    }
}