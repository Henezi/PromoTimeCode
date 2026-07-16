package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.managers.SaveManager;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Перезагрузить плагин";
    }

    @Override
    public String getUsage() {
        return "/promo reload";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("promotimecode.admin") || sender.isOp();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        SaveManager saveManager = plugin.getSaveManager();
        saveManager.reload();

        plugin.getPlaytimeTask().reload();

        sendMessage(sender, lang.getRaw("messages.reloaded"));

        return true;
    }
}