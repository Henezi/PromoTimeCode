package com.yourname.promotimecode;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class MainCommand implements CommandExecutor {

    private FileConfiguration config;

    private String getMsg(String key) {
        return config.getString("messages." + key, "&cСообщение не найдено: " + key);
    }

    private List<String> getMsgList(String key) {
        return config.getStringList("messages." + key);
    }

    private String getPrefix() {
        return config.getString("messages.prefix", "&6[Promo] &r");
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        config = PromoTimeCode.getInstance().getConfig();

        if (args.length == 0) {
            sender.sendMessage(color(getPrefix() + getMsg("help-header")));
            sender.sendMessage(color("&e/" + label + " help &7- " + getMsg("help-help")));
            sender.sendMessage(color("&e/" + label + " <код> &7- " + getMsg("help-activate")));
            sender.sendMessage(color("&e/" + label + " progress &7- " + getMsg("help-progress")));
            return true;
        }

        // --- HELP ---
        if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(color(getPrefix() + getMsg("help-header")));
            sender.sendMessage(color("&e/" + label + " <код> &7- " + getMsg("help-activate")));
            sender.sendMessage(color("&e/" + label + " progress &7- " + getMsg("help-progress")));
            sender.sendMessage(color("&e/" + label + " help &7- " + getMsg("help-help")));
            if (sender.hasPermission("promotimecode.youtube") || sender.isOp()) {
                sender.sendMessage(color("&e/" + label + " create <код> &7- " + getMsg("help-create")));
                sender.sendMessage(color("&e/" + label + " info &7- " + getMsg("help-info")));
            }
            if (sender.hasPermission("promotimecode.admin") || sender.isOp()) {
                sender.sendMessage(color("&e/" + label + " reload &7- " + getMsg("help-reload")));
                sender.sendMessage(color("&e/" + label + " show <код> &7- " + getMsg("help-show")));
                sender.sendMessage(color("&e/" + label + " balance add/remove <код> <сумма> &7- " + getMsg("help-balance")));
                sender.sendMessage(color("&e/" + label + " delete <код> &7- " + getMsg("help-delete")));
                sender.sendMessage(color("&e/" + label + " ipstats <игрок> &7- Информация по IP"));
            }
            return true;
        }

        // --- PROGRESS ---
        if (args[0].equalsIgnoreCase("progress")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(color(getPrefix() + getMsg("only-player")));
                return true;
            }
            Player p = (Player) sender;
            DataManager.PlayerData data = PromoTimeCode.getInstance().getDataManager().getPlayerData(p);

            if (data.activeCode == null) {
                p.sendMessage(color(getPrefix() + getMsg("no-active-code")));
                return true;
            }

            CodeManager.PromoCode code = PromoTimeCode.getInstance().getCodeManager().loadCode(data.activeCode);
            if (code == null) {
                p.sendMessage(color(getPrefix() + "&cОшибка! Промокод не найден."));
                return true;
            }

            int elapsedMinutes = data.playedSeconds / 60;
            int current = Math.min(elapsedMinutes, code.requiredPlaytime);
            int needed = code.requiredPlaytime;

            p.sendMessage(color(getMsg("progress-title")
                    .replace("%code%", data.activeCode)));
            p.sendMessage(color(getMsg("progress-played")
                    .replace("%current%", String.valueOf(current))
                    .replace("%need%", String.valueOf(needed))));
            p.sendMessage(color(data.redeemed ? getMsg("progress-status-done") : getMsg("progress-status-waiting")));
            return true;
        }

        // --- CREATE ---
        if (args[0].equalsIgnoreCase("create") && args.length >= 2) {
            if (!sender.hasPermission("promotimecode.youtube") && !sender.isOp()) {
                sender.sendMessage(color(getPrefix() + getMsg("no-permission")));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(color(getPrefix() + getMsg("only-player")));
                return true;
            }

            Player p = (Player) sender;
            String playerName = p.getName();

            if (config.getBoolean("settings.one-code-per-youtuber", true)) {
                CodeManager.PromoCode existingCode = PromoTimeCode.getInstance().getCodeManager().findCodeByAuthor(playerName);
                if (existingCode != null && !sender.isOp()) {
                    sender.sendMessage(color(getPrefix() + getMsg("you-already-have-code")
                            .replace("%code%", existingCode.code)));
                    sender.sendMessage(color(getPrefix() + getMsg("your-code-info")));
                    return true;
                }
            }

            String code = args[1].toUpperCase();
            String pattern = config.getString("formats.code-pattern", "^[A-Z0-9]{3,20}$");
            if (!code.matches(pattern)) {
                sender.sendMessage(color(getPrefix() + "&cКод должен содержать только буквы и цифры (3-20 символов)!"));
                return true;
            }

            if (PromoTimeCode.getInstance().getCodeManager().loadCode(code) != null) {
                sender.sendMessage(color(getPrefix() + getMsg("code-already-exists")
                        .replace("%code%", code)));
                return true;
            }

            int duration = config.getInt("settings.default-duration-minutes", 60);
            double reward = config.getDouble("settings.default-balance", 50.0);
            int requiredTime = config.getInt("settings.default-playtime-minutes", 120);
            String group = config.getString("extra.default-group", "VIP");

            boolean success = PromoTimeCode.getInstance().getCodeManager().createCode(
                    code, group, duration, reward, requiredTime, sender.getName()
            );

            if (success) {
                sender.sendMessage(color(getPrefix() + getMsg("code-created")
                        .replace("%code%", code)));

                List<String> details = getMsgList("code-created-details");
                if (details.isEmpty()) {
                    // Если нет деталей в конфиге - показываем стандартные
                    sender.sendMessage(color("&7├─ &eГруппа: &f" + group + " &7на &f" + duration + " &7мин"));
                    sender.sendMessage(color("&7├─ &eНаграда ютуберу: &f" + reward + " &7монет"));
                    sender.sendMessage(color("&7└─ &eНужно отыграть: &f" + requiredTime + " &7минут"));
                } else {
                    for (String detail : details) {
                        detail = detail.replace("%group%", group)
                                .replace("%duration%", String.valueOf(duration))
                                .replace("%reward%", String.valueOf(reward))
                                .replace("%playtime%", String.valueOf(requiredTime));
                        sender.sendMessage(color(detail));
                    }
                }

                sender.sendMessage(color("&c&l" + getMsg("cannot-activate-own-code")));
                sender.sendMessage(color("&7" + getMsg("cannot-activate-own-code-hint")));
            } else {
                sender.sendMessage(color(getPrefix() + "&cОшибка при создании кода!"));
            }
            return true;
        }

        // --- DELETE (админ) ---
        if (args[0].equalsIgnoreCase("delete") && args.length >= 2) {
            if (!sender.hasPermission("promotimecode.admin") && !sender.isOp()) {
                sender.sendMessage(color(getPrefix() + getMsg("no-permission")));
                return true;
            }
            String code = args[1].toUpperCase();
            if (PromoTimeCode.getInstance().getCodeManager().deleteCode(code)) {
                sender.sendMessage(color(getPrefix() + getMsg("code-deleted")
                        .replace("%code%", code)));
            } else {
                sender.sendMessage(color(getPrefix() + getMsg("code-delete-error")));
            }
            return true;
        }

        // --- INFO (для ютубера) ---
        if (args[0].equalsIgnoreCase("info")) {
            if (!sender.hasPermission("promotimecode.youtube") && !sender.isOp()) {
                sender.sendMessage(color(getPrefix() + getMsg("no-permission")));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(color(getPrefix() + getMsg("only-player")));
                return true;
            }

            Player p = (Player) sender;
            CodeManager.PromoCode foundCode = PromoTimeCode.getInstance().getCodeManager()
                    .findCodeByAuthor(p.getName());

            if (foundCode == null) {
                sender.sendMessage(color(getPrefix() + getMsg("no-codes-found")));
                return true;
            }

            sender.sendMessage(color(getMsg("info-title")
                    .replace("%code%", foundCode.code)));
            sender.sendMessage(color(getMsg("info-group")
                    .replace("%group%", foundCode.groupName)));
            sender.sendMessage(color(getMsg("info-duration")
                    .replace("%duration%", String.valueOf(foundCode.durationMinutes))));
            sender.sendMessage(color(getMsg("info-reward")
                    .replace("%reward%", String.valueOf(foundCode.rewardMoney))));
            sender.sendMessage(color(getMsg("info-playtime")
                    .replace("%playtime%", String.valueOf(foundCode.requiredPlaytime))));
            sender.sendMessage(color(getMsg("info-players")
                    .replace("%players%", String.valueOf(foundCode.players.size()))));
            sender.sendMessage(color(getMsg("info-completed")
                    .replace("%completed%", String.valueOf(foundCode.completedPlayers))));
            sender.sendMessage(color(getMsg("info-balance")
                    .replace("%balance%", String.valueOf(foundCode.balance))));
            return true;
        }

        // --- RELOAD ---
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("promotimecode.admin") && !sender.isOp()) {
                sender.sendMessage(color(getPrefix() + getMsg("no-permission")));
                return true;
            }

            PromoTimeCode.getInstance().getDataManager().saveAll();
            AntiAbuseManager.getInstance().saveIpData();

            PromoTimeCode.getInstance().reloadConfig();
            PromoTimeCode.getInstance().getDataManager().load();
            AntiAbuseManager.getInstance().reload();

            sender.sendMessage(color(getPrefix() + getMsg("reloaded")));
            return true;
        }

        // --- SHOW (админ) ---
        if (args[0].equalsIgnoreCase("show") && args.length >= 2) {
            if (!sender.hasPermission("promotimecode.admin") && !sender.isOp()) {
                sender.sendMessage(color(getPrefix() + getMsg("no-permission")));
                return true;
            }
            String code = args[1].toUpperCase();
            CodeManager.PromoCode promoCode = PromoTimeCode.getInstance().getCodeManager().loadCode(code);
            if (promoCode == null) {
                sender.sendMessage(color(getPrefix() + getMsg("code-not-found")
                        .replace("%code%", code)));
                return true;
            }

            sender.sendMessage(color(getMsg("show-title")
                    .replace("%code%", promoCode.code)));
            sender.sendMessage(color(getMsg("show-author")
                    .replace("%author%", promoCode.author)));
            sender.sendMessage(color(getMsg("show-group")
                    .replace("%group%", promoCode.groupName)));
            sender.sendMessage(color(getMsg("show-duration")
                    .replace("%duration%", String.valueOf(promoCode.durationMinutes))));
            sender.sendMessage(color(getMsg("show-reward")
                    .replace("%reward%", String.valueOf(promoCode.rewardMoney))));
            sender.sendMessage(color(getMsg("show-playtime")
                    .replace("%playtime%", String.valueOf(promoCode.requiredPlaytime))));
            sender.sendMessage(color(getMsg("show-players")
                    .replace("%players%", String.valueOf(promoCode.players.size()))));
            sender.sendMessage(color(getMsg("show-completed")
                    .replace("%completed%", String.valueOf(promoCode.completedPlayers))));
            sender.sendMessage(color(getMsg("show-balance")
                    .replace("%balance%", String.valueOf(promoCode.balance))));
            return true;
        }

        // --- IP STATS (админ) ---
        if (args[0].equalsIgnoreCase("ipstats") && args.length >= 2) {
            if (!sender.hasPermission("promotimecode.admin") && !sender.isOp()) {
                sender.sendMessage(color(getPrefix() + getMsg("no-permission")));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color("&cИгрок не найден!"));
                return true;
            }
            String stats = AntiAbuseManager.getInstance().getIpStats(target);
            sender.sendMessage(color("&6IP статистика для &e" + target.getName() + "&6:"));
            sender.sendMessage(color("&7" + stats));
            return true;
        }

        // --- BALANCE ---
        if (args[0].equalsIgnoreCase("balance") && args.length >= 4) {
            if (!sender.hasPermission("promotimecode.admin") && !sender.isOp()) {
                sender.sendMessage(color(getPrefix() + getMsg("no-permission")));
                return true;
            }
            String action = args[1].toLowerCase();
            String code = args[2].toUpperCase();
            double amount;

            try {
                amount = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(color(getPrefix() + getMsg("balance-invalid-amount")));
                return true;
            }

            CodeManager.PromoCode promoCode = PromoTimeCode.getInstance().getCodeManager().loadCode(code);
            if (promoCode == null) {
                sender.sendMessage(color(getPrefix() + getMsg("code-not-found")
                        .replace("%code%", code)));
                return true;
            }

            if (action.equals("add")) {
                promoCode.balance += amount;
                sender.sendMessage(color(getPrefix() + getMsg("balance-added")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%code%", code)));
            } else if (action.equals("remove")) {
                promoCode.balance = Math.max(0, promoCode.balance - amount);
                sender.sendMessage(color(getPrefix() + getMsg("balance-removed")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%code%", code)));
            } else {
                sender.sendMessage(color(getPrefix() + getMsg("balance-invalid-action")));
                return true;
            }

            PromoTimeCode.getInstance().getCodeManager().saveCode(promoCode);
            sender.sendMessage(color(getMsg("balance-current")
                    .replace("%balance%", String.valueOf(promoCode.balance))));
            return true;
        }

        // --- АКТИВАЦИЯ КОДА ---
        if (args.length == 1 && sender instanceof Player) {
            String firstArg = args[0].toLowerCase();

            List<String> reservedCommands = Arrays.asList("help", "create", "reload", "show", "progress", "info", "balance", "delete");
            if (!reservedCommands.contains(firstArg)) {

                Player p = (Player) sender;
                String code = args[0].toUpperCase();

                CodeManager.PromoCode promoCode = PromoTimeCode.getInstance().getCodeManager().loadCode(code);
                if (promoCode == null) {
                    p.sendMessage(color(getPrefix() + getMsg("code-not-found")
                            .replace("%code%", code)));
                    return true;
                }

                if (promoCode.author.equalsIgnoreCase(p.getName())) {
                    p.sendMessage(color(getPrefix() + getMsg("cannot-activate-own-code")));
                    p.sendMessage(color("&7" + getMsg("cannot-activate-own-code-hint")));
                    return true;
                }

                DataManager.PlayerData data = PromoTimeCode.getInstance().getDataManager().getPlayerData(p);
                if (data.activeCode != null) {
                    p.sendMessage(color(getPrefix() + getMsg("already-has-code")
                            .replace("%code%", data.activeCode)));
                    p.sendMessage(color("&7" + getMsg("already-has-code-hint")));
                    return true;
                }

                boolean success = PromoTimeCode.getInstance().getCodeManager().activateCode(p, code);
                if (success) {
                    p.sendMessage(color(getPrefix() + getMsg("code-activated")
                            .replace("%code%", code)));
                    p.sendMessage(color("&7" + getMsg("code-activated-hint")
                            .replace("%time%", String.valueOf(promoCode.requiredPlaytime))));
                } else {
                    p.sendMessage(color(getPrefix() + "&cОшибка при активации кода!"));
                }
                return true;
            }
        }

        sender.sendMessage(color(getPrefix() + "&cНеизвестная команда. Используйте /" + label + " help"));
        return true;
    }
}