package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.PromoCode;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BalanceCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "balance";
    }

    @Override
    public String getDescription() {
        return "Изменить баланс промокода";
    }

    @Override
    public String getUsage() {
        return "/promo balance add/remove <code> <amount>";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("promotimecode.admin") || sender.isOp();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, plugin.getLanguageManager().getRaw("commands.balance"));
            return true;
        }

        LanguageManager lang = plugin.getLanguageManager();
        CodeManager codeManager = plugin.getCodeManager();

        String action = args[0].toLowerCase();
        String code = args[1].toUpperCase();
        double amount;

        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sendMessage(sender, lang.getRaw("balance.invalid-amount"));
            return true;
        }

        PromoCode promoCode = codeManager.getCode(code);
        if (promoCode == null) {
            sendMessage(sender, lang.getRaw("messages.code-not-found"));
            return true;
        }

        if (action.equals("add")) {
            promoCode.addBalance(amount);
            sendMessage(sender, lang.getRaw("balance.added")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%code%", code));
        } else if (action.equals("remove")) {
            promoCode.setBalance(Math.max(0, promoCode.getBalance() - amount));
            sendMessage(sender, lang.getRaw("balance.removed")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%code%", code));
        } else {
            sendMessage(sender, lang.getRaw("balance.invalid-action"));
            return true;
        }

        codeManager.saveCode(code);
        sendMessage(sender, lang.getRaw("balance.current")
                .replace("%balance%", String.valueOf(promoCode.getBalance())));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("add", "remove"));
        } else if (args.length == 2) {
            CodeManager codeManager = plugin.getCodeManager();
            for (String code : codeManager.getCodes().keySet()) {
                suggestions.add(code);
            }
        }

        return suggestions;
    }
}