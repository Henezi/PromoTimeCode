package com.kvassuk.promotimecode.tasks;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.DataManager;
import com.kvassuk.promotimecode.data.PlayerData;
import com.kvassuk.promotimecode.data.PromoCode;
import com.kvassuk.promotimecode.managers.AntiAbuseManager;
import com.kvassuk.promotimecode.managers.RewardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PlaytimeTask extends BukkitRunnable {

    private final PromoTimeCode plugin;
    private final DataManager dataManager;
    private final CodeManager codeManager;
    private final AntiAbuseManager antiAbuse;
    private final RewardManager rewardManager;
    private int checkInterval;

    public PlaytimeTask(PromoTimeCode plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.codeManager = plugin.getCodeManager();
        this.antiAbuse = plugin.getAntiAbuseManager();
        this.rewardManager = plugin.getRewardManager();
        this.checkInterval = plugin.getConfigManager().getCheckInterval();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                processPlayer(player);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка обработки игрока " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private void processPlayer(Player player) {
        PlayerData data = dataManager.getPlayerData(player);

        // Проверка: есть ли активный код
        if (!data.hasActiveCode()) return;

        // Проверка: уже получил награду
        if (data.isRedeemed()) return;

        // Проверка: код существует
        PromoCode code = codeManager.getCode(data.getActiveCode());
        if (code == null) {
            data.setActiveCode(null);
            data.setDirty(true);
            return;
        }

        // Проверка AFK
        if (!antiAbuse.shouldCountTime(player)) {
            // Игрок AFK, время не считается
            return;
        }

        // Увеличение времени
        data.addPlayedSeconds(checkInterval);

        // Проверка: выполнено ли условие
        if (data.isEligibleForReward(code.getRequiredPlaytime())) {
            // Выдача награды
            rewardManager.processCompletion(player, code, data);
        }

        // Сохранение изменений
        dataManager.markDirty(player.getUniqueId());
    }

    public void reload() {
        this.checkInterval = plugin.getConfigManager().getCheckInterval();
    }
}