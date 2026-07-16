package com.kvassuk.promotimecode.managers;

import com.kvassuk.promotimecode.PromoTimeCode;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class SaveManager {

    private final PromoTimeCode plugin;
    private BukkitTask autoSaveTask;
    private static final int SAVE_INTERVAL = 60 * 20; // 60 секунд в тиках

    public SaveManager(PromoTimeCode plugin) {
        this.plugin = plugin;
    }

    public void startAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveAll();
        }, SAVE_INTERVAL, SAVE_INTERVAL);

        plugin.getLogger().info("Автосохранение запущено (интервал: 60 сек)");
    }

    public void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    public void saveAll() {
        long start = System.currentTimeMillis();

        // Сохраняем данные игроков
        plugin.getDataManager().saveAllDirty();

        // Сохраняем промокоды
        plugin.getCodeManager().saveAllDirty();

        // Сохраняем IP данные (если есть изменения)
        plugin.getAntiAbuseManager().saveIpDataIfDirty();

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Сохранение выполнено за " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    public void reload() {
        // Сохраняем всё перед перезагрузкой
        saveAll();

        // Перезагружаем конфиги
        plugin.getConfigManager().loadAll();

        // Перезагружаем язык
        plugin.getLanguageManager().loadLanguage(plugin.getConfigManager().getLanguage());

        // Перезагружаем данные
        plugin.getDataManager().loadAll();
        plugin.getCodeManager().loadAll();

        // Перезагружаем AntiAbuse
        plugin.getAntiAbuseManager().reload();

        // Перезапускаем автосохранение
        startAutoSave();

        plugin.getLogger().info("Плагин перезагружен!");
    }
}