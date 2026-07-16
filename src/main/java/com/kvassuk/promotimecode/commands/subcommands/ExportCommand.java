package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.CodeManager;
import com.kvassuk.promotimecode.data.PromoCode;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ExportCommand implements SubCommand {

    private final PromoTimeCode plugin = PromoTimeCode.getInstance();

    @Override
    public String getName() {
        return "export";
    }

    @Override
    public String getDescription() {
        return "Экспорт данных в CSV";
    }

    @Override
    public String getUsage() {
        return "/promo export";
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

        File exportFolder = new File(plugin.getDataFolder(), "exports");
        if (!exportFolder.exists()) {
            exportFolder.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File csvFile = new File(exportFolder, "export_" + timestamp + ".csv");

        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Code,Author,Group,Duration (min),Reward (coins),Required Playtime (min),Players,Completed,Balance\n");

            for (PromoCode code : codes.values()) {
                String line = String.format("%s,%s,%s,%d,%.2f,%d,%d,%d,%.2f\n",
                        code.getCode(),
                        code.getAuthor(),
                        code.getGroupName(),
                        code.getDurationMinutes(),
                        code.getRewardMoney(),
                        code.getRequiredPlaytime(),
                        code.getPlayerCount(),
                        code.getCompletedPlayers(),
                        code.getBalance()
                );
                writer.write(line);
            }

            writer.flush();

            sendMessage(sender, lang.getRaw("messages.export-success")
                    .replace("%file%", csvFile.getName()));

        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка экспорта: " + e.getMessage());
            sendMessage(sender, lang.getRaw("messages.export-error"));
            e.printStackTrace();
        }

        return true;
    }
}