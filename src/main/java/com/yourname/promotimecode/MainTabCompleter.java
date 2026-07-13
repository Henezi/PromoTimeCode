package com.yourname.promotimecode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainTabCompleter implements TabCompleter {

    // Базовый список команд (можно расширять)
    private static final List<String> COMMANDS = Arrays.asList(
            "help", "create", "info", "progress", "show", "reload", "balance", "delete"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String cmd : COMMANDS) {
                // Проверяем права на команды
                if (cmd.equals("create") && !sender.hasPermission("promotimecode.youtube") && !sender.isOp()) {
                    continue;
                }
                if ((cmd.equals("show") || cmd.equals("reload") || cmd.equals("balance") || cmd.equals("delete"))
                        && !sender.hasPermission("promotimecode.admin") && !sender.isOp()) {
                    continue;
                }
                if (cmd.startsWith(partial)) {
                    suggestions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            String firstArg = args[0].toLowerCase();
            String partial = args[1].toLowerCase();

            // Для команды balance - на втором аргументе показываем add/remove
            if (firstArg.equals("balance")) {
                if ("add".startsWith(partial)) suggestions.add("add");
                if ("remove".startsWith(partial)) suggestions.add("remove");
            }

            // Для команды delete - на втором аргументе показываем коды
            if (firstArg.equals("delete")) {
                if (sender.hasPermission("promotimecode.admin") || sender.isOp()) {
                    addCodeSuggestions(suggestions, partial);
                }
            }

            // Для команды show - на втором аргументе показываем коды
            if (firstArg.equals("show")) {
                if (sender.hasPermission("promotimecode.admin") || sender.isOp()) {
                    addCodeSuggestions(suggestions, partial);
                }
            }

        } else if (args.length == 3) {
            String firstArg = args[0].toLowerCase();
            String secondArg = args[1].toLowerCase();
            String partial = args[2].toLowerCase();

            // Для balance add/remove - на третьем аргументе показываем коды
            if (firstArg.equals("balance") && (secondArg.equals("add") || secondArg.equals("remove"))) {
                if (sender.hasPermission("promotimecode.admin") || sender.isOp()) {
                    addCodeSuggestions(suggestions, partial);
                }
            }
        }

        return suggestions;
    }

    // ===== ВСПОМОГАТЕЛЬНЫЙ МЕТОД: Добавить коды в подсказки =====
    private void addCodeSuggestions(List<String> suggestions, String partial) {
        File codesFolder = new File(PromoTimeCode.getInstance().getDataFolder(), "codes");
        File[] files = codesFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                String codeName = file.getName().replace(".json", "");
                if (codeName.toLowerCase().startsWith(partial.toLowerCase())) {
                    suggestions.add(codeName);
                }
            }
        }
    }
}