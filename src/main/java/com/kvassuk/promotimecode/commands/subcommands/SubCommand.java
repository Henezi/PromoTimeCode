package com.kvassuk.promotimecode.commands.subcommands;

import com.kvassuk.promotimecode.PromoTimeCode;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public interface SubCommand {

    String getName();

    String getDescription();

    String getUsage();

    boolean hasPermission(CommandSender sender);

    boolean execute(CommandSender sender, String[] args);

    default List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    default Player getPlayer(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    default void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        MiniMessage miniMessage = MiniMessage.miniMessage();
        Audience audience = PromoTimeCode.getInstance().adventure().sender(sender);
        Component component = miniMessage.deserialize(message);
        audience.sendMessage(component);
    }

    default void sendMessage(CommandSender sender, String message, String... placeholders) {
        if (message == null || message.isEmpty()) return;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
            }
        }
        sendMessage(sender, message);
    }
}