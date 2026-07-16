package com.kvassuk.promotimecode;

import com.kvassuk.promotimecode.commands.PromoCommand;
import com.kvassuk.promotimecode.config.ConfigManager;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.DataManager;
import com.kvassuk.promotimecode.hooks.PlaceholderAPIHook;
import com.kvassuk.promotimecode.managers.AntiAbuseManager;
import com.kvassuk.promotimecode.managers.BackupManager;
import com.kvassuk.promotimecode.managers.RewardManager;
import com.kvassuk.promotimecode.managers.SaveManager;
import com.kvassuk.promotimecode.tasks.PlaytimeTask;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class PromoTimeCode extends JavaPlugin {

    private static PromoTimeCode instance;
    private BukkitAudiences adventure;
    private boolean isPlaceholderApiEnabled = false;

    // Менеджеры
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private DataManager dataManager;
    private CodeManager codeManager;
    private SaveManager saveManager;
    private AntiAbuseManager antiAbuseManager;
    private RewardManager rewardManager;
    private BackupManager backupManager;

    // Задачи
    private PlaytimeTask playtimeTask;

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        // Инициализация Adventure (для MiniMessage)
        adventure = BukkitAudiences.create(this);

        // Создание папок
        createFolders();

        // Загрузка конфигов
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // Загрузка языков
        languageManager = new LanguageManager(this);
        languageManager.loadLanguage(configManager.getLanguage());

        // Инициализация менеджеров
        dataManager = new DataManager(this);
        codeManager = new CodeManager(this);
        antiAbuseManager = new AntiAbuseManager(this);
        rewardManager = new RewardManager(this);

        // ===== ИНИЦИАЛИЗАЦИЯ BACKUPMANAGER =====
        backupManager = new BackupManager(this);

        // ===== АВТО-БЭКАП ПРИ ЗАПУСКЕ =====
        if (configManager.isBackupOnStartup()) {
            backupManager.createFullBackup();
            getLogger().info("Авто-бэкап создан при запуске!");
        }

        // Регистрация слушателей для AntiAbuse
        getServer().getPluginManager().registerEvents(antiAbuseManager, this);

        // Загрузка данных
        dataManager.loadAll();
        codeManager.loadAll();

        // Инициализация SaveManager (сохранение каждые 60 секунд)
        saveManager = new SaveManager(this);
        saveManager.startAutoSave();

        // Регистрация команд
        getCommand("promo").setExecutor(new PromoCommand(this));
        getCommand("promo").setTabCompleter(new PromoCommand(this));

        // Запуск задачи проверки времени
        int checkInterval = configManager.getCheckInterval();
        playtimeTask = new PlaytimeTask(this);
        playtimeTask.runTaskTimerAsynchronously(this, 0L, checkInterval * 20L);

        // Регистрация PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            isPlaceholderApiEnabled = true;
            new PlaceholderAPIHook(this).register();
            getLogger().info("PlaceholderAPI успешно зарегистрирован!");
        }

        long loadTime = System.currentTimeMillis() - startTime;
        printStartupMessage(loadTime);
    }

    private void createFolders() {
        File[] folders = {
                getDataFolder(),
                new File(getDataFolder(), "playerdata"),
                new File(getDataFolder(), "codes"),
                new File(getDataFolder(), "logs"),
                new File(getDataFolder(), "exports"),
                new File(getDataFolder(), "backups"),
                new File(getDataFolder(), "languages")
        };

        for (File folder : folders) {
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
    }

    private void printStartupMessage(long loadTime) {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§8§m----------------------------------------------");
        Bukkit.getConsoleSender().sendMessage("  §6§l╔══════════════════════════════════════════════╗");
        Bukkit.getConsoleSender().sendMessage("  §6§l║ §ePromoTimeCode §7v" + getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage("  §6§l║ §fРазработчик: §eKvassuk");
        Bukkit.getConsoleSender().sendMessage("  §6§l║ §fСтатус: §a✅ Успешно загружен §7(" + loadTime + "ms)");
        Bukkit.getConsoleSender().sendMessage("  §6§l║ §fЯзык: §e" + configManager.getLanguage());
        Bukkit.getConsoleSender().sendMessage("  §6§l║ §fPlaceholderAPI: §e" + (isPlaceholderApiEnabled ? "✅ Включён" : "❌ Не найден"));
        Bukkit.getConsoleSender().sendMessage("  §6§l║ §fКоды загружено: §e" + codeManager.getCodeCount());
        Bukkit.getConsoleSender().sendMessage("  §6§l║ §fИгроков загружено: §e" + dataManager.getPlayerCount());
        Bukkit.getConsoleSender().sendMessage("  §6§l╚══════════════════════════════════════════════╝");
        Bukkit.getConsoleSender().sendMessage("§8§m----------------------------------------------");
        Bukkit.getConsoleSender().sendMessage("");
    }

    @Override
    public void onDisable() {
        if (saveManager != null) {
            saveManager.stopAutoSave();
            saveManager.saveAll();
        }

        if (playtimeTask != null) {
            playtimeTask.cancel();
        }

        if (adventure != null) {
            adventure.close();
        }

        getLogger().info("PromoTimeCode v2 выключен!");
    }

    public static PromoTimeCode getInstance() {
        return instance;
    }

    public BukkitAudiences adventure() {
        return adventure;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CodeManager getCodeManager() {
        return codeManager;
    }

    public SaveManager getSaveManager() {
        return saveManager;
    }

    public AntiAbuseManager getAntiAbuseManager() {
        return antiAbuseManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }

    public boolean isPlaceholderApiEnabled() {
        return isPlaceholderApiEnabled;
    }

    public PlaytimeTask getPlaytimeTask() {
        return playtimeTask;
    }
}