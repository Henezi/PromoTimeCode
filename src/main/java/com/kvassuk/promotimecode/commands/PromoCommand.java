package com.kvassuk.promotimecode.commands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.commands.subcommands.*;
import com.kvassuk.promotimecode.config.LanguageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PromoCommand implements CommandExecutor, TabCompleter {

    private final PromoTimeCode plugin;
    private final MiniMessage miniMessage;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public PromoCommand(PromoTimeCode plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();

        registerSubCommand(new CreateCommand());
        registerSubCommand(new DeleteCommand());
        registerSubCommand(new ReloadCommand());
        registerSubCommand(new InfoCommand());
        registerSubCommand(new ProgressCommand());
        registerSubCommand(new ActivateCommand());
        registerSubCommand(new TopCommand());
        registerSubCommand(new BalanceCommand());
        registerSubCommand(new ExportCommand());
        registerSubCommand(new GuiCommand());
        registerSubCommand(new IpstatsCommand());
        registerSubCommand(new ShowCommand());
        registerSubCommand(new BackupCommand());
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (args.length == 0) {
            sendHelp(sender, 1);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, 1);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            if (sender instanceof Player) {
                String code = args[0].toUpperCase();
                ActivateCommand activate = (ActivateCommand) subCommands.get("activate");
                if (activate != null) {
                    return activate.execute(sender, new String[]{code});
                }
            }
            sendMessage(sender, lang.getRaw("messages.code-not-found"));
            return true;
        }

        if (!subCommand.hasPermission(sender)) {
            sendMessage(sender, lang.getRaw("messages.no-permission"));
            return true;
        }

        try {
            return subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        } catch (Exception e) {
            sendMessage(sender, "<red>Ошибка выполнения команды!</red>");
            plugin.getLogger().severe("Ошибка в команде " + subCommandName + ": " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    private void sendHelp(CommandSender sender, int page) {
        LanguageManager lang = plugin.getLanguageManager();
        boolean isAdmin = sender.hasPermission("promotimecode.admin") || sender.isOp();
        boolean isYouTuber = sender.hasPermission("promotimecode.youtube") || isAdmin;

        List<String> helpLines = new ArrayList<>();
        helpLines.add(lang.getRaw("help.header"));

        helpLines.add(lang.getRaw("help.player"));
        helpLines.add(lang.getRaw("commands.help"));
        helpLines.add(lang.getRaw("commands.activate"));
        helpLines.add(lang.getRaw("commands.progress"));

        if (isYouTuber) {
            helpLines.add(lang.getRaw("help.youtuber"));
            helpLines.add(lang.getRaw("commands.create"));
            helpLines.add(lang.getRaw("commands.info"));
        }

        if (isAdmin) {
            helpLines.add(lang.getRaw("help.admin"));
            helpLines.add(lang.getRaw("commands.show"));
            helpLines.add(lang.getRaw("commands.balance"));
            helpLines.add(lang.getRaw("commands.delete"));
            helpLines.add(lang.getRaw("commands.reload"));
            helpLines.add(lang.getRaw("commands.ipstats"));
            helpLines.add(lang.getRaw("commands.top"));
            helpLines.add(lang.getRaw("commands.export"));
            helpLines.add(lang.getRaw("commands.gui"));
            helpLines.add(lang.getRaw("commands.backup"));  // ← ДОБАВИТЬ
        }

        for (String line : helpLines) {
            sendMessage(sender, line);
        }
    }

    private void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        plugin.adventure().sender(sender).sendMessage(miniMessage.deserialize(message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("help".startsWith(partial)) {
                completions.add("help");
            }
            for (String cmd : subCommands.keySet()) {
                SubCommand sub = subCommands.get(cmd);
                if (sub.hasPermission(sender) && cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length >= 2) {
            String subName = args[0].toLowerCase();
            SubCommand sub = subCommands.get(subName);
            if (sub != null && sub.hasPermission(sender)) {
                completions = sub.onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    public PromoTimeCode getPlugin() {
        return plugin;
    }
}