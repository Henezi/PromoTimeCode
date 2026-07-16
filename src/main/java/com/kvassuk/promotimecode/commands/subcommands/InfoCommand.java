package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.PromoCode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class InfoCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Информация о вашем промокоде";
    }

    @Override
    public String getUsage() {
        return "/promo info";
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

        LanguageManager lang = plugin.getLanguageManager();
        CodeManager codeManager = plugin.getCodeManager();

        PromoCode foundCode = codeManager.findCodeByAuthor(player.getName());

        if (foundCode == null) {
            sendMessage(sender, lang.getRaw("messages.no-codes-found"));
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("code", foundCode.getCode());
        placeholders.put("group", foundCode.getGroupName());
        placeholders.put("duration", String.valueOf(foundCode.getDurationMinutes()));
        placeholders.put("reward", String.valueOf(foundCode.getRewardMoney()));
        placeholders.put("playtime", String.valueOf(foundCode.getRequiredPlaytime()));
        placeholders.put("players", String.valueOf(foundCode.getPlayerCount()));
        placeholders.put("completed", String.valueOf(foundCode.getCompletedPlayers()));
        placeholders.put("balance", String.valueOf(foundCode.getBalance()));

        sendMessage(sender, lang.getRaw("info.title", placeholders));
        sendMessage(sender, lang.getRaw("info.group", placeholders));
        sendMessage(sender, lang.getRaw("info.duration", placeholders));
        sendMessage(sender, lang.getRaw("info.reward", placeholders));
        sendMessage(sender, lang.getRaw("info.playtime", placeholders));
        sendMessage(sender, lang.getRaw("info.players", placeholders));
        sendMessage(sender, lang.getRaw("info.completed", placeholders));
        sendMessage(sender, lang.getRaw("info.balance", placeholders));

        return true;
    }
}