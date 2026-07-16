package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.PromoCode;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShowCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "show";
    }

    @Override
    public String getDescription() {
        return "Информация о промокоде";
    }

    @Override
    public String getUsage() {
        return "/promo show <code>";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("promotimecode.admin") || sender.isOp();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, plugin.getLanguageManager().getRaw("commands.show"));
            return true;
        }

        LanguageManager lang = plugin.getLanguageManager();
        CodeManager codeManager = plugin.getCodeManager();

        String code = args[0].toUpperCase();
        PromoCode promoCode = codeManager.getCode(code);

        if (promoCode == null) {
            sendMessage(sender, lang.getRaw("messages.code-not-found"));
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("code", promoCode.getCode());
        placeholders.put("author", promoCode.getAuthor());
        placeholders.put("group", promoCode.getGroupName());
        placeholders.put("duration", String.valueOf(promoCode.getDurationMinutes()));
        placeholders.put("reward", String.valueOf(promoCode.getRewardMoney()));
        placeholders.put("playtime", String.valueOf(promoCode.getRequiredPlaytime()));
        placeholders.put("players", String.valueOf(promoCode.getPlayerCount()));
        placeholders.put("completed", String.valueOf(promoCode.getCompletedPlayers()));
        placeholders.put("balance", String.valueOf(promoCode.getBalance()));

        sendMessage(sender, lang.getRaw("show.title", placeholders));
        sendMessage(sender, lang.getRaw("show.author", placeholders));
        sendMessage(sender, lang.getRaw("show.group", placeholders));
        sendMessage(sender, lang.getRaw("show.duration", placeholders));
        sendMessage(sender, lang.getRaw("show.reward", placeholders));
        sendMessage(sender, lang.getRaw("show.playtime", placeholders));
        sendMessage(sender, lang.getRaw("show.players", placeholders));
        sendMessage(sender, lang.getRaw("show.completed", placeholders));
        sendMessage(sender, lang.getRaw("show.balance", placeholders));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            CodeManager codeManager = plugin.getCodeManager();
            String partial = args[0].toUpperCase();
            for (String code : codeManager.getCodes().keySet()) {
                if (code.startsWith(partial)) {
                    suggestions.add(code);
                }
            }
        }
        return suggestions;
    }
}