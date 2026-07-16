package com.kvassuk.promotimecode.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kvassuk.promotimecode.PromoTimeCode;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiAbuseManager implements Listener {

    private final PromoTimeCode plugin;
    private final Gson gson;
    private final File ipDataFile;
    private final File abuseLogFile;

    private final Map<String, List<String>> ipActivations = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> playerIsAfk = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerLastMove = new ConcurrentHashMap<>();

    private int maxActivationsPerIp;
    private int afkThresholdSeconds;
    private boolean enableAfkCheck;
    private boolean enableIpLimit;
    private int afkWarningCooldown;
    private boolean isDirty = false;

    public AntiAbuseManager(PromoTimeCode plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.ipDataFile = new File(plugin.getDataFolder(), "ip_data.json");
        this.abuseLogFile = new File(plugin.getDataFolder(), "logs/abuse_log.txt");

        loadConfig();
        loadIpData();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getAntiAbuseConfig();
        if (config == null) return;

        maxActivationsPerIp = config.getInt("max-activations-per-ip", 3);
        afkThresholdSeconds = config.getInt("afk-threshold-seconds", 60);
        enableAfkCheck = config.getBoolean("enable-afk-check", true);
        enableIpLimit = config.getBoolean("enable-ip-limit", true);
        afkWarningCooldown = config.getInt("afk-warning-cooldown", 30);
    }

    private void loadIpData() {
        if (!ipDataFile.exists()) return;

        try (FileReader reader = new FileReader(ipDataFile)) {
            Map<String, List<String>> loaded = gson.fromJson(reader, Map.class);
            if (loaded != null) {
                ipActivations.clear();
                ipActivations.putAll(loaded);
                plugin.getLogger().info("Загружено " + ipActivations.size() + " IP-записей");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось загрузить IP-данные: " + e.getMessage());
        }
    }

    public void saveIpDataIfDirty() {
        if (!isDirty) return;

        try (FileWriter writer = new FileWriter(ipDataFile)) {
            gson.toJson(ipActivations, writer);
            isDirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить IP-данные: " + e.getMessage());
        }
    }

    public boolean checkIpLimit(Player player) {
        if (!enableIpLimit) return true;

        String ip = getPlayerIp(player);
        if (ip == null) return true;

        List<String> players = ipActivations.computeIfAbsent(ip, k -> new ArrayList<>());

        if (players.contains(player.getName())) {
            return true;
        }

        if (players.size() >= maxActivationsPerIp) {
            logAbuse(player, "IP limit exceeded: " + ip + " already has " + players.size() + " activations");
            return false;
        }

        return true;
    }

    public void registerActivation(Player player) {
        if (!enableIpLimit) return;

        String ip = getPlayerIp(player);
        if (ip == null) return;

        List<String> players = ipActivations.computeIfAbsent(ip, k -> new ArrayList<>());
        if (!players.contains(player.getName())) {
            players.add(player.getName());
            isDirty = true;
        }
    }

    public boolean shouldCountTime(Player player) {
        if (!enableAfkCheck) return true;

        UUID uuid = player.getUniqueId();
        Long lastMove = playerLastMove.get(uuid);

        if (lastMove == null) {
            playerLastMove.put(uuid, System.currentTimeMillis());
            playerIsAfk.put(uuid, false);
            return true;
        }

        long elapsed = (System.currentTimeMillis() - lastMove) / 1000;

        if (elapsed > afkThresholdSeconds) {
            playerIsAfk.put(uuid, true);

            long lastWarning = player.hasMetadata("promo-afk-last-warning")
                    ? player.getMetadata("promo-afk-last-warning").get(0).asLong()
                    : 0;

            long currentTime = System.currentTimeMillis();
            int warningCooldownMillis = afkWarningCooldown * 1000;

            if (currentTime - lastWarning > warningCooldownMillis) {
                player.sendMessage("§c⚠ Вы выглядите как AFK! Таймер промокода §lПРИОСТАНОВЛЕН§c!");
                player.sendMessage("§7Пошевелитесь, чтобы продолжить отсчёт времени.");
                player.setMetadata("promo-afk-last-warning",
                        new FixedMetadataValue(plugin, currentTime));
            }

            return false;
        }

        playerIsAfk.put(uuid, false);

        if (player.hasMetadata("promo-afk-last-warning")) {
            player.sendMessage("§a✅ Вы снова активны! Отсчёт времени возобновлён.");
            player.removeMetadata("promo-afk-last-warning", plugin);
        }

        return true;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enableAfkCheck) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            playerLastMove.put(uuid, System.currentTimeMillis());
            playerIsAfk.put(uuid, false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerIsAfk.remove(uuid);
        playerLastMove.remove(uuid);
    }

    private String getPlayerIp(Player player) {
        try {
            return player.getAddress().getAddress().getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private void logAbuse(Player player, String reason) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logEntry = String.format("[%s] %s (%s) - %s%n",
                timestamp, player.getName(), player.getUniqueId(), reason);

        try (FileWriter writer = new FileWriter(abuseLogFile, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось записать лог нарушения: " + e.getMessage());
        }

        plugin.getLogger().warning("[AntiAbuse] " + player.getName() + " - " + reason);
    }

    public String getIpStats(Player player) {
        String ip = getPlayerIp(player);
        if (ip == null) return "IP не определён";

        List<String> players = ipActivations.get(ip);
        if (players == null || players.isEmpty()) {
            return "Нет активаций с этого IP";
        }

        return String.format("IP %s активировал код у %d игроков: %s",
                ip, players.size(), String.join(", ", players));
    }

    public int getActivationsFromIp(Player player) {
        String ip = getPlayerIp(player);
        if (ip == null) return 0;

        List<String> players = ipActivations.get(ip);
        return players == null ? 0 : players.size();
    }

    public int getMaxActivationsPerIp() {
        return maxActivationsPerIp;
    }

    public void reload() {
        loadConfig();
        loadIpData();
        plugin.getLogger().info("AntiAbuse перезагружен");
    }
}