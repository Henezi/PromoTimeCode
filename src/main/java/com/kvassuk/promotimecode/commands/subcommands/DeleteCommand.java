package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.CodeManager;
import org.bukkit.command.CommandSender;

public class DeleteCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Удалить промокод";
    }

    @Override
    public String getUsage() {
        return "/promo delete <code>";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("promotimecode.admin") || sender.isOp();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, plugin.getLanguageManager().getRaw("commands.delete"));
            return true;
        }

        LanguageManager lang = plugin.getLanguageManager();
        CodeManager codeManager = plugin.getCodeManager();

        String code = args[0].toUpperCase();

        if (!codeManager.codeExists(code)) {
            sendMessage(sender, lang.getRaw("messages.code-not-found"));
            return true;
        }

        if (codeManager.deleteCode(code)) {
            sendMessage(sender, lang.getRaw("messages.code-deleted").replace("%code%", code));
        } else {
            sendMessage(sender, lang.getRaw("messages.code-delete-error"));
        }

        return true;
    }
}