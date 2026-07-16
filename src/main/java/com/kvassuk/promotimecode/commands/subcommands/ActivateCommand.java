package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.DataManager;
import com.kvassuk.promotimecode.data.PlayerData;
import com.kvassuk.promotimecode.data.PromoCode;
import com.kvassuk.promotimecode.managers.AntiAbuseManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ActivateCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "activate";
    }

    @Override
    public String getDescription() {
        return "Активировать промокод";
    }

    @Override
    public String getUsage() {
        return "/promo activate <code> или /promo <code>";
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

        if (args.length < 1) {
            sendMessage(sender, plugin.getLanguageManager().getRaw("commands.activate"));
            return true;
        }

        String code = args[0].toUpperCase();
        LanguageManager lang = plugin.getLanguageManager();
        DataManager dataManager = plugin.getDataManager();
        CodeManager codeManager = plugin.getCodeManager();
        AntiAbuseManager antiAbuse = plugin.getAntiAbuseManager();

        PromoCode promoCode = codeManager.getCode(code);
        if (promoCode == null) {
            sendMessage(sender, lang.getRaw("messages.code-not-found"));
            return true;
        }

        if (promoCode.getAuthor().equalsIgnoreCase(player.getName())) {
            String msg1 = lang.getRaw("messages.cannot-activate-own-code");
            String msg2 = lang.getRaw("messages.cannot-activate-own-code-hint");

            if (!msg1.contains("не найдено") && !msg1.contains("not found")) {
                sendMessage(sender, msg1);
            } else {
                sendMessage(sender, "<red>Вы не можете активировать свой собственный промокод!</red>");
            }

            if (!msg2.contains("не найдено") && !msg2.contains("not found")) {
                sendMessage(sender, msg2);
            } else {
                sendMessage(sender, "<gray>Это сделано для предотвращения накрутки</gray>");
            }
            return true;
        }

        // Проверка IP лимита
        if (!antiAbuse.checkIpLimit(player)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("max", String.valueOf(antiAbuse.getMaxActivationsPerIp()));
            sendMessage(sender, lang.getRaw("messages.ip-limit-block", placeholders));
            sendMessage(sender, lang.getRaw("messages.ip-limit-block-hint", placeholders));
            return true;
        }

        // Проверка: есть ли уже активный код
        PlayerData playerData = dataManager.getPlayerData(player);
        if (playerData.hasActiveCode()) {
            sendMessage(sender, lang.getRaw("messages.already-has-code")
                    .replace("%code%", playerData.getActiveCode()));
            sendMessage(sender, lang.getRaw("messages.already-has-code-hint"));
            return true;
        }

        // Активация кода
        playerData.setActiveCode(code);
        playerData.setPlayedSeconds(0);
        playerData.setRedeemed(false);
        playerData.setActivationIp(player.getAddress().getAddress().getHostAddress());
        playerData.setActivationTimestamp(System.currentTimeMillis());

        antiAbuse.registerActivation(player);
        promoCode.addPlayer(player.getUniqueId());

        dataManager.markDirty(player.getUniqueId());
        codeManager.saveCode(code);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("code", code);
        placeholders.put("time", String.valueOf(promoCode.getRequiredPlaytime()));

        sendMessage(sender, lang.getRaw("messages.code-activated", placeholders));
        sendMessage(sender, lang.getRaw("messages.code-activated-hint", placeholders));

        return true;
    }
}