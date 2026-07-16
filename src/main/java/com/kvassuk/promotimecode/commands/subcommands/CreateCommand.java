package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.ConfigManager;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.PromoCode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Создать промокод";
    }

    @Override
    public String getUsage() {
        return "/promo create <code>";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("promotimecode.youtube") || sender.isOp();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) {
            sendMessage(sender, plugin.getLanguageManager().getRaw("messages.only-player"));
            return true;
        }

        if (args.length < 1) {
            sendMessage(sender, plugin.getLanguageManager().getRaw("commands.create"));
            return true;
        }

        LanguageManager lang = plugin.getLanguageManager();
        ConfigManager config = plugin.getConfigManager();
        CodeManager codeManager = plugin.getCodeManager();

        String code = args[0].toUpperCase();

        // Проверка формата кода
        if (!code.matches("^[A-Z0-9]{3,20}$")) {
            sendMessage(sender, "<red>Код должен содержать только буквы и цифры (3-20 символов)!</red>");
            return true;
        }

        // Проверка: существует ли уже такой код
        if (codeManager.codeExists(code)) {
            sendMessage(sender, lang.getRaw("messages.code-already-exists"));
            return true;
        }

        // Проверка: есть ли уже код у этого ютубера
        if (config.getMainConfig().getBoolean("settings.one-code-per-youtuber", true)) {
            PromoCode existing = codeManager.findCodeByAuthor(player.getName());
            if (existing != null && !player.isOp()) {
                sendMessage(sender, lang.getRaw("messages.you-already-have-code")
                        .replace("%code%", existing.getCode()));
                sendMessage(sender, lang.getRaw("messages.your-code-info"));
                return true;
            }
        }

        // Получение настроек из конфига
        int duration = config.getMainConfig().getInt("settings.default-duration-minutes", 60);
        double reward = config.getMainConfig().getDouble("settings.default-balance", 50.0);
        int requiredTime = config.getMainConfig().getInt("settings.default-playtime-minutes", 120);
        String group = config.getMainConfig().getString("extra.default-group", "VIP");

        // Создание кода
        boolean success = codeManager.createCode(code, group, duration, reward, requiredTime, player.getName());

        if (success) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("code", code);
            placeholders.put("group", group);
            placeholders.put("duration", String.valueOf(duration));
            placeholders.put("reward", String.valueOf(reward));
            placeholders.put("playtime", String.valueOf(requiredTime));

            sendMessage(sender, lang.getRaw("messages.code-created", placeholders));

            List<String> details = lang.getRawList("messages.code-created-details");
            for (String detail : details) {
                String formatted = detail
                        .replace("%group%", group)
                        .replace("%duration%", String.valueOf(duration))
                        .replace("%reward%", String.valueOf(reward))
                        .replace("%playtime%", String.valueOf(requiredTime));
                sendMessage(sender, formatted);
            }

            sendMessage(sender, lang.getRaw("messages.cannot-activate-own-code"));
            sendMessage(sender, lang.getRaw("messages.cannot-activate-own-code-hint"));
        } else {
            sendMessage(sender, "<red>Ошибка при создании кода!</red>");
        }

        return true;
    }
}