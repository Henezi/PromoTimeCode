package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.PromoCode;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TopCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();
    private final DecimalFormat df = new DecimalFormat("#0.00");

    @Override
    public String getName() {
        return "top";
    }

    @Override
    public String getDescription() {
        return "ТОП промокодов";
    }

    @Override
    public String getUsage() {
        return "/promo top";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("promotimecode.admin") || sender.isOp();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();
        CodeManager codeManager = plugin.getCodeManager();

        Map<String, PromoCode> codes = codeManager.getCodes();
        if (codes.isEmpty()) {
            sendMessage(sender, lang.getRaw("top.no-codes"));
            return true;
        }

        List<PromoCode> sorted = new ArrayList<>(codes.values());
        sorted.sort(Comparator.comparingInt(PromoCode::getCompletedPlayers).reversed());

        sendMessage(sender, lang.getRaw("top.title"));

        int limit = Math.min(10, sorted.size());
        for (int i = 0; i < limit; i++) {
            PromoCode code = sorted.get(i);
            int index = i + 1;

            // Простая строка без выравнивания
            String line = "<gray>" + index + ". " + code.getCode() + " — " + code.getAuthor() +
                    " | Игроков: " + code.getPlayerCount() +
                    " | Выполнили: " + code.getCompletedPlayers() +
                    " | Баланс: " + df.format(code.getBalance()) + "</gray>";
            sendMessage(sender, line);
        }

        return true;
    }
}