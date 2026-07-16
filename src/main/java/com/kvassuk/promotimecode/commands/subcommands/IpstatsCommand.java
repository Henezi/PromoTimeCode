package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.managers.AntiAbuseManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class IpstatsCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "ipstats";
    }

    @Override
    public String getDescription() {
        return "Статистика по IP";
    }

    @Override
    public String getUsage() {
        return "/promo ipstats <player>";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("promotimecode.admin") || sender.isOp();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, plugin.getLanguageManager().getRaw("commands.ipstats"));
            return true;
        }

        LanguageManager lang = plugin.getLanguageManager();
        AntiAbuseManager antiAbuse = plugin.getAntiAbuseManager();

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sendMessage(sender, lang.getRaw("messages.player-not-found"));
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());

        String stats = antiAbuse.getIpStats(target);
        int activations = antiAbuse.getActivationsFromIp(target);

        sendMessage(sender, lang.getRaw("ipstats.title", placeholders));
        sendMessage(sender, lang.getRaw("ipstats.info").replace("%info%", stats));

        placeholders.put("count", String.valueOf(activations));
        sendMessage(sender, lang.getRaw("ipstats.activations", placeholders));

        return true;
    }
}