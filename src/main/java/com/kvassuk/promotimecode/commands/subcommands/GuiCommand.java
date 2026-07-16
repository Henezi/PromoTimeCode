package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuiCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public String getDescription() {
        return "Открыть админ панель";
    }

    @Override
    public String getUsage() {
        return "/promo gui";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("promotimecode.admin") || sender.isOp();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) {
            sendMessage(sender, plugin.getLanguageManager().getRaw("messages.only-player"));
            return true;
        }

        sendMessage(sender, "<gold>=== PromoTimeCode GUI ===</gold>");
        sendMessage(sender, "<gray>GUI в разработке! Используйте команды:</gray>");
        sendMessage(sender, "<gray>/promo show <code> - просмотр кода</gray>");
        sendMessage(sender, "<gray>/promo top - ТОП кодов</gray>");
        sendMessage(sender, "<gray>/promo export - экспорт данных</gray>");

        return true;
    }
}