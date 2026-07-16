package com.kvassuk.promotimecode.config;

import com.kvassuk.promotimecode.PromoTimeCode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {

    private final PromoTimeCode plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, Object> messages = new HashMap<>();
    private String currentLanguage;

    public LanguageManager(PromoTimeCode plugin) {
        this.plugin = plugin;
        createDefaultLanguageFiles();
    }

    private void createDefaultLanguageFiles() {
        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String[] defaultFiles = {"en.yml", "ru.yml"};
        for (String fileName : defaultFiles) {
            File langFile = new File(langFolder, fileName);
            if (!langFile.exists()) {
                try (InputStream is = plugin.getResource(fileName)) {
                    if (is != null) {
                        Files.copy(is, langFile.toPath());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Не удалось создать " + fileName);
                }
            }
        }
    }

    public void loadLanguage(String language) {
        this.currentLanguage = language;
        messages.clear();

        File langFile = new File(plugin.getDataFolder(), "languages/" + language + ".yml");
        if (!langFile.exists()) {
            langFile = new File(plugin.getDataFolder(), "languages/en.yml");
            plugin.getLogger().warning("Язык " + language + " не найден, используется en");
        }

        try {
            FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
            for (String key : langConfig.getKeys(true)) {
                if (langConfig.isString(key)) {
                    messages.put(key, langConfig.getString(key));
                } else if (langConfig.isList(key)) {
                    messages.put(key, langConfig.getStringList(key));
                }
            }
            plugin.getLogger().info("Загружено сообщений: " + messages.size() + " (язык: " + language + ")");
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка загрузки языка: " + e.getMessage());
        }
    }

    public String getRaw(String key) {
        Object value = messages.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return "&cСообщение не найдено: " + key;
    }

    @SuppressWarnings("unchecked")
    public List<String> getRawList(String key) {
        Object value = messages.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return new ArrayList<>();
    }

    public Component get(String key) {
        String raw = getRaw(key);
        return miniMessage.deserialize(raw);
    }

    public Component get(String key, Map<String, String> placeholders) {
        String raw = getRaw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return miniMessage.deserialize(raw);
    }

    public String getRaw(String key, Map<String, String> placeholders) {
        String raw = getRaw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return raw;
    }

    public String getLanguage() {
        return currentLanguage;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}