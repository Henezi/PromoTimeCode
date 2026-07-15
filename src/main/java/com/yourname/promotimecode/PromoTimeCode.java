package com.yourname.promotimecode;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class PromoTimeCode extends JavaPlugin implements Listener {

    private static PromoTimeCode instance;
    private DataManager dataManager;
    private CodeManager codeManager;
    private PlaytimeChecker playtimeChecker;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        saveDefaultConfig();

        this.dataManager = new DataManager();
        this.codeManager = new CodeManager();

        MainCommand mainCommand = new MainCommand();
        MainTabCompleter tabCompleter = new MainTabCompleter();

        if (getCommand("promocode") != null) {
            getCommand("promocode").setExecutor(mainCommand);
            getCommand("promocode").setTabCompleter(tabCompleter);
        }
        if (getCommand("promo") != null) {
            getCommand("promo").setExecutor(mainCommand);
            getCommand("promo").setTabCompleter(tabCompleter);
        }

        getServer().getPluginManager().registerEvents(this, this);

        AntiAbuseManager antiAbuse = new AntiAbuseManager();
        getServer().getPluginManager().registerEvents(antiAbuse, this);

        int checkInterval = getConfig().getInt("settings.check-interval-seconds", 10);
        long ticks = checkInterval * 20L;

        this.playtimeChecker = new PlaytimeChecker();
        playtimeChecker.runTaskTimer(this, 0L, ticks);

        long loadTime = System.currentTimeMillis() - startTime;
        printStartupMessage(loadTime);
    }

    private void printStartupMessage(long loadTime) {
        String version = getDescription().getVersion();

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8&m-----------------------------------------------------"
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &6&l╔══════════════════════════════════════════════════╗"
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &6&l║ &ePromoTimeCode &7v" + version
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &6&l║ &fРазработчик: &eKvassuk"
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &6&l║ &fСтатус: &a✅ Успешно загружен &7(" + loadTime + "ms)"
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &6&l╚══════════════════════════════════════════════════╝"
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8&m-----------------------------------------------------"
        ));
        Bukkit.getConsoleSender().sendMessage("");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        if (AntiAbuseManager.getInstance() != null) {
            AntiAbuseManager.getInstance().saveIpData();
        }
        if (playtimeChecker != null) {
            playtimeChecker.cancel();
        }

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8&m-----------------------------------------------------"
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &6&l╔══════════════════════════════════════════════════╗"
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &6&l║ &c❌ PromoTimeCode &7v" + getDescription().getVersion() + " &cвыключен"
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &6&l║ &fДанные сохранены: &a✅"
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &6&l╚══════════════════════════════════════════════════╝"
        ));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8&m-----------------------------------------------------"
        ));
        Bukkit.getConsoleSender().sendMessage("");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (dataManager != null && getConfig().getBoolean("extra.save-on-quit", true)) {
            dataManager.saveAll();
        }
    }

    public static PromoTimeCode getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CodeManager getCodeManager() {
        return codeManager;
    }
}