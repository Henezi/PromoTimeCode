package com.yourname.promotimecode;

import org.bukkit.Bukkit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AntiAbuseManager implements Listener {

    private static AntiAbuseManager instance;
    private final Map<String, List<String>> ipActivations = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> playerIsAfk = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerLastMove = new ConcurrentHashMap<>();
    private final File abuseLogFile;
    private final File ipDataFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private int maxActivationsPerIp;
    private int afkThresholdSeconds;
    private boolean enableAfkCheck;
    private boolean enableIpLimit;
    private int afkWarningCooldown;

    public AntiAbuseManager() {
        instance = this;
        abuseLogFile = new File(PromoTimeCode.getInstance().getDataFolder(), "abuse_log.txt");
        ipDataFile = new File(PromoTimeCode.getInstance().getDataFolder(), "ip_data.json");
        loadConfig();
        loadIpData();
    }

    private void loadConfig() {
        FileConfiguration config = PromoTimeCode.getInstance().getConfig();
        maxActivationsPerIp = config.getInt("anti-abuse.max-activations-per-ip", 3);
        afkThresholdSeconds = config.getInt("anti-abuse.afk-threshold-seconds", 60);
        enableAfkCheck = config.getBoolean("anti-abuse.enable-afk-check", true);
        enableIpLimit = config.getBoolean("anti-abuse.enable-ip-limit", true);
        afkWarningCooldown = config.getInt("anti-abuse.afk-warning-cooldown", 30);
    }

    public void saveIpData() {
        try (FileWriter writer = new FileWriter(ipDataFile)) {
            gson.toJson(ipActivations, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadIpData() {
        if (!ipDataFile.exists()) return;
        try (FileReader reader = new FileReader(ipDataFile)) {
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                ipActivations.clear();
                ipActivations.putAll(loaded);
                Bukkit.getLogger().info("[PromoTimeCode] Загружено " + ipActivations.size() + " IP-записей");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static AntiAbuseManager getInstance() {
        return instance;
    }

    public boolean checkIpLimit(Player player) {
        if (!enableIpLimit) return true;

        String ip = getPlayerIp(player);
        if (ip == null) return true;

        List<String> players = ipActivations.computeIfAbsent(ip, k -> new ArrayList<>());

        if (players.contains(player.getName())) {
            return true;
        }

        int currentActivations = players.size();

        if (currentActivations >= maxActivationsPerIp) {
            logAbuse(player, "IP limit exceeded: " + ip + " already has " + currentActivations + " activations. Max: " + maxActivationsPerIp);

            player.sendMessage("§cВы не можете активировать код! С этого IP уже активировано максимальное количество аккаунтов (" + maxActivationsPerIp + ").");
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
            saveIpData();
        }
    }

    public boolean isPlayerAfk(Player player) {
        if (!enableAfkCheck) return false;
        return playerIsAfk.getOrDefault(player.getUniqueId(), false);
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
                        new org.bukkit.metadata.FixedMetadataValue(PromoTimeCode.getInstance(), currentTime));
            }

            return false;
        }

        playerIsAfk.put(uuid, false);

        if (player.hasMetadata("promo-afk-last-warning")) {
            player.sendMessage("§a✅ Вы снова активны! Отсчёт времени возобновлён.");
            player.removeMetadata("promo-afk-last-warning", PromoTimeCode.getInstance());
        }

        return true;
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
        String logEntry = String.format("[%s] %s (%s) - %s%n", timestamp, player.getName(), player.getUniqueId(), reason);

        try (FileWriter writer = new FileWriter(abuseLogFile, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Bukkit.getLogger().warning("[PromoTimeCode] ПОДОЗРИТЕЛЬНАЯ АКТИВАЦИЯ: " + player.getName() + " - " + reason);
    }

    public String getIpStats(Player player) {
        String ip = getPlayerIp(player);
        if (ip == null) return "IP не определён";
        List<String> players = ipActivations.get(ip);
        if (players == null) return "Нет активаций с этого IP";
        return String.format("IP %s активировал код у %d игроков: %s", ip, players.size(), String.join(", ", players));
    }

    public void reload() {
        loadConfig();
        loadIpData();
        Bukkit.getLogger().info("[PromoTimeCode] AntiAbuse перезагружен, данные сохранены");
    }

    public List<String> getAllPlayersWithIp() {
        Set<String> allPlayers = new HashSet<>();
        for (List<String> players : ipActivations.values()) {
            allPlayers.addAll(players);
        }
        return new ArrayList<>(allPlayers);
    }
}