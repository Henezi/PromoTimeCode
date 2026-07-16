package com.kvassuk.promotimecode.config;

import com.kvassuk.promotimecode.PromoTimeCode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final PromoTimeCode plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private FileConfiguration mainConfig;
    private FileConfiguration antiAbuseConfig;
    private FileConfiguration rewardsConfig;

    private String language;
    private int checkInterval;
    private int saveInterval;
    private boolean debugMode;

    public ConfigManager(PromoTimeCode plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        // Загружаем все конфиги
        loadConfig("config.yml");
        loadConfig("antiabuse.yml");
        loadConfig("rewards.yml");

        // Получаем доступ к каждому конфигу
        mainConfig = configs.get("config.yml");
        antiAbuseConfig = configs.get("antiabuse.yml");
        rewardsConfig = configs.get("rewards.yml");

        // Если основного конфига нет — создаём
        if (mainConfig == null) {
            plugin.saveDefaultConfig();
            mainConfig = plugin.getConfig();
            configs.put("config.yml", mainConfig);
        }

        // Если antiabuse.yml отсутствует — создаём из ресурсов
        if (antiAbuseConfig == null) {
            plugin.saveResource("antiabuse.yml", false);
            antiAbuseConfig = YamlConfiguration.loadConfiguration(
                    new File(plugin.getDataFolder(), "antiabuse.yml"));
            configs.put("antiabuse.yml", antiAbuseConfig);
        }

        // Если rewards.yml отсутствует — создаём из ресурсов
        if (rewardsConfig == null) {
            plugin.saveResource("rewards.yml", false);
            rewardsConfig = YamlConfiguration.loadConfiguration(
                    new File(plugin.getDataFolder(), "rewards.yml"));
            configs.put("rewards.yml", rewardsConfig);
        }

        // Загрузка параметров из основного конфига
        language = mainConfig.getString("settings.language", "en");
        checkInterval = mainConfig.getInt("settings.check-interval-seconds", 10);
        saveInterval = mainConfig.getInt("settings.save-interval-seconds", 60);
        debugMode = mainConfig.getBoolean("settings.debug-mode", false);
    }

    private void loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (configFile.exists()) {
            configs.put(fileName, YamlConfiguration.loadConfiguration(configFile));
        }
    }

    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public FileConfiguration getAntiAbuseConfig() {
        return antiAbuseConfig;
    }

    public FileConfiguration getRewardsConfig() {
        return rewardsConfig;
    }

    public void reload() {
        configs.clear();
        loadAll();
    }

    public String getLanguage() {
        return language;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public int getSaveInterval() {
        return saveInterval;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isBackupOnStartup() {
        return mainConfig.getBoolean("settings.backup-on-startup", true);
    }

    public File getConfigFile(String fileName) {
        return new File(plugin.getDataFolder(), fileName);
    }
}