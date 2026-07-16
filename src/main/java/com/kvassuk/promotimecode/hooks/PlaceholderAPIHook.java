package com.kvassuk.promotimecode.hooks;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.DataManager;
import com.kvassuk.promotimecode.data.PlayerData;
import com.kvassuk.promotimecode.data.PromoCode;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final PromoTimeCode plugin;

    public PlaceholderAPIHook(PromoTimeCode plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "promocode";
    }

    @Override
    public String getAuthor() {
        return "Kvassuk";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        DataManager dataManager = plugin.getDataManager();
        CodeManager codeManager = plugin.getCodeManager();
        PlayerData data = dataManager.getPlayerData(player);

        switch (identifier.toLowerCase()) {
            case "code":
                return data.hasActiveCode() ? data.getActiveCode() : "Нет кода";

            case "progress":
                if (!data.hasActiveCode()) return "0%";
                PromoCode code = codeManager.getCode(data.getActiveCode());
                if (code == null) return "0%";
                int progress = (int) ((double) data.getPlayedSeconds() / (code.getRequiredPlaytime() * 60) * 100);
                return Math.min(progress, 100) + "%";

            case "time":
                if (!data.hasActiveCode()) return "0";
                return String.valueOf(data.getElapsedMinutes());

            case "balance":
                if (!data.hasActiveCode()) return "0";
                PromoCode codeBalance = codeManager.getCode(data.getActiveCode());
                if (codeBalance == null) return "0";
                return String.format("%.2f", codeBalance.getBalance());

            case "completed":
                if (!data.hasActiveCode()) return "0";
                PromoCode codeCompleted = codeManager.getCode(data.getActiveCode());
                if (codeCompleted == null) return "0";
                return String.valueOf(codeCompleted.getCompletedPlayers());

            case "author":
                if (!data.hasActiveCode()) return "Нет автора";
                PromoCode codeAuthor = codeManager.getCode(data.getActiveCode());
                if (codeAuthor == null) return "Нет автора";
                return codeAuthor.getAuthor();

            case "status":
                if (!data.hasActiveCode()) return "Нет кода";
                if (data.isRedeemed()) return "✅ Награда получена";
                PromoCode codeStatus = codeManager.getCode(data.getActiveCode());
                if (codeStatus == null) return "Ошибка";
                if (data.getElapsedMinutes() >= codeStatus.getRequiredPlaytime()) {
                    return "⏳ Ожидание награды";
                }
                return "⏳ В процессе";

            case "time_required":
                if (!data.hasActiveCode()) return "0";
                PromoCode codeTime = codeManager.getCode(data.getActiveCode());
                if (codeTime == null) return "0";
                return String.valueOf(codeTime.getRequiredPlaytime());

            default:
                return null;
        }
    }
}