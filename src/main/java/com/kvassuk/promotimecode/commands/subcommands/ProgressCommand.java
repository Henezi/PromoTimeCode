package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.DataManager;
import com.kvassuk.promotimecode.data.PlayerData;
import com.kvassuk.promotimecode.data.PromoCode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ProgressCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "progress";
    }

    @Override
    public String getDescription() {
        return "Показать прогресс выполнения";
    }

    @Override
    public String getUsage() {
        return "/promo progress";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) {
            sendMessage(sender, plugin.getLanguageManager().getRaw("messages.only-player"));
            return true;
        }

        LanguageManager lang = plugin.getLanguageManager();
        DataManager dataManager = plugin.getDataManager();
        CodeManager codeManager = plugin.getCodeManager();

        PlayerData data = dataManager.getPlayerData(player);

        if (!data.hasActiveCode()) {
            sendMessage(sender, lang.getRaw("messages.no-active-code"));
            return true;
        }

        PromoCode code = codeManager.getCode(data.getActiveCode());
        if (code == null) {
            sendMessage(sender, lang.getRaw("messages.code-not-found"));
            return true;
        }

        int elapsedMinutes = data.getElapsedMinutes();
        int current = Math.min(elapsedMinutes, code.getRequiredPlaytime());
        int needed = code.getRequiredPlaytime();
        boolean redeemed = data.isRedeemed();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("code", data.getActiveCode());
        placeholders.put("current", String.valueOf(current));
        placeholders.put("need", String.valueOf(needed));

        sendMessage(sender, lang.getRaw("progress.title", placeholders));
        sendMessage(sender, lang.getRaw("progress.played", placeholders));

        String statusKey = redeemed ? "progress.status-done" : "progress.status-waiting";
        sendMessage(sender, lang.getRaw(statusKey));

        return true;
    }
}