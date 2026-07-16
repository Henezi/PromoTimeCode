package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.managers.BackupManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class BackupCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "backup";
    }

    @Override
    public String getDescription() {
        return "Управление бэкапами";
    }

    @Override
    public String getUsage() {
        return "/promo backup [create|list|restore <name>]";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("promotimecode.admin") || sender.isOp();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        BackupManager backupManager = plugin.getBackupManager();

        if (args.length == 0 || args[0].equalsIgnoreCase("create")) {
            // Создание бэкапа
            String name = backupManager.createFullBackup();
            if (name != null) {
                sendMessage(sender, "<green>Бэкап создан: <gold>" + name + "</gold></green>");
            } else {
                sendMessage(sender, "<red>Ошибка создания бэкапа!</red>");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            // Список бэкапов
            String[] backups = backupManager.getBackupList();
            if (backups.length == 0) {
                sendMessage(sender, "<gray>Нет сохранённых бэкапов</gray>");
            } else {
                sendMessage(sender, "<gold>=== Список бэкапов ===</gold>");
                String latest = backupManager.getLatestBackup();
                for (String name : backups) {
                    String marker = name.equals(latest) ? " <green>(последний)</green>" : "";
                    sendMessage(sender, "<gray>- <gold>" + name + "</gold>" + marker);
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("restore") && args.length >= 2) {
            // Восстановление бэкапа
            String backupName = args[1];
            sendMessage(sender, "<yellow>Восстановление бэкапа " + backupName + "...</yellow>");
            sendMessage(sender, "<red>Сервер может зависнуть на время восстановления!</red>");

            boolean success = backupManager.restoreBackup(backupName);
            if (success) {
                sendMessage(sender, "<green>Бэкап <gold>" + backupName + "</gold> восстановлен!</green>");
                sendMessage(sender, "<green>Плагин перезагружен автоматически.</green>");
            } else {
                sendMessage(sender, "<red>Ошибка восстановления бэкапа!</red>");
            }
            return true;
        }

        sendMessage(sender, "<gray>Использование:</gray>");
        sendMessage(sender, "<gray>  /promo backup create - создать бэкап</gray>");
        sendMessage(sender, "<gray>  /promo backup list - список бэкапов</gray>");
        sendMessage(sender, "<gray>  /promo backup restore <имя> - восстановить бэкап</gray>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("create");
            suggestions.add("list");
            suggestions.add("restore");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("restore")) {
            String[] backups = plugin.getBackupManager().getBackupList();
            for (String name : backups) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }
}