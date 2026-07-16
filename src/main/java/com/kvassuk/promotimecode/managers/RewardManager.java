package com.kvassuk.promotimecode.managers;

import com.kvassuk.promotimecode.PromoTimeCode;
import com.kvassuk.promotimecode.config.LanguageManager;
import com.kvassuk.promotimecode.data.PlayerData;
import com.kvassuk.promotimecode.data.PromoCode;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RewardManager {

    private final PromoTimeCode plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration rewardsConfig;

    public RewardManager(PromoTimeCode plugin) {
        this.plugin = plugin;
        this.rewardsConfig = plugin.getConfigManager().getRewardsConfig();
    }

    private void reloadConfig() {
        this.rewardsConfig = plugin.getConfigManager().getRewardsConfig();
    }

    public void giveReward(Player player, PromoCode code) {
        reloadConfig();

        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(player.getUniqueId());

            if (user == null) {
                plugin.getLogger().warning("Не удалось найти пользователя " + player.getName() + " в LuckPerms");
                return;
            }

            String groupName = code.getGroupName().toLowerCase();
            long durationMillis = TimeUnit.MINUTES.toMillis(code.getDurationMinutes());
            Instant expiry = Instant.now().plusMillis(durationMillis);

            boolean hasGroup = user.getCachedData().getPermissionData()
                    .checkPermission("group." + groupName).asBoolean();

            if (hasGroup) {
                sendMessage(player, "<gold>У вас уже есть группа " + groupName +
                        "! Время продлено на " + code.getDurationMinutes() + " минут.</gold>");
            }

            user.data().add(Node.builder("group." + groupName)
                    .expiry(expiry)
                    .build());

            api.getUserManager().saveUser(user);

            playRewardEffects(player);

            sendMessage(player, "<green>Вам выдана группа <gold>" + groupName +
                    "</gold> <green>на <gold>" + code.getDurationMinutes() + "</gold> <green>минут!</green>");

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Игроку " + player.getName() +
                        " выдана группа " + groupName + " на " + code.getDurationMinutes() + " минут");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при выдаче награды: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMessage(Player player, String message) {
        Audience audience = plugin.adventure().player(player);
        Component component = miniMessage.deserialize(message);
        audience.sendMessage(component);
    }

    private void playRewardEffects(Player player) {
        if (rewardsConfig == null) return;
        Location loc = player.getLocation();

        if (rewardsConfig.getBoolean("rewards.enable-sound", true)) {
            String soundName = rewardsConfig.getString("rewards.sound", "ENTITY_PLAYER_LEVELUP");
            float volume = (float) rewardsConfig.getDouble("rewards.sound-volume", 1.0);
            float pitch = (float) rewardsConfig.getDouble("rewards.sound-pitch", 1.0);

            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(loc, sound, volume, pitch);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестный звук: " + soundName);
            }
        }

        if (rewardsConfig.getBoolean("rewards.enable-particles", true)) {
            String particleName = rewardsConfig.getString("rewards.particle", "FIREWORK");
            int count = rewardsConfig.getInt("rewards.particle-count", 30);

            try {
                Particle particle = Particle.valueOf(particleName);
                player.getWorld().spawnParticle(particle, loc.clone().add(0, 1, 0), count, 0.5, 0.5, 0.5, 0.1);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неизвестная частица: " + particleName);
            }
        }

        if (rewardsConfig.getBoolean("rewards.enable-title", true)) {
            String title = rewardsConfig.getString("rewards.title-text", "&a&lНАГРАДА ПОЛУЧЕНА!");
            String subtitle = rewardsConfig.getString("rewards.subtitle-text", "&eВы получили группу!");
            int fadeIn = rewardsConfig.getInt("rewards.title-fade-in", 10);
            int stay = rewardsConfig.getInt("rewards.title-stay", 40);
            int fadeOut = rewardsConfig.getInt("rewards.title-fade-out", 10);

            player.sendTitle(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', title),
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', subtitle),
                    fadeIn, stay, fadeOut
            );
        }
    }

    public void processCompletion(Player player, PromoCode code, PlayerData data) {
        giveReward(player, code);

        code.addBalance(code.getRewardMoney());
        code.incrementCompletedPlayers();

        data.setRedeemed(true);
        data.setDirty(true);

        plugin.getCodeManager().saveCode(code.getCode());
        plugin.getDataManager().markDirty(player.getUniqueId());

        LanguageManager lang = plugin.getLanguageManager();
        String prefix = lang.getRaw("prefix");
        String doneMsg = lang.getRaw("reward.done");

        sendMessage(player, prefix + doneMsg);

        sendMessage(player, "<gray>├─ <gold>Группа:</gold> <white>" + code.getGroupName() +
                " <gray>на <gold>" + code.getDurationMinutes() + "</gold> <gray>мин</gray>");
        sendMessage(player, "<gray>└─ <gold>Ютубер получил награду:</gold> <white>" +
                code.getRewardMoney() + "</white> <gray>монет</gray>");

        plugin.getLogger().info("Игрок " + player.getName() +
                " выполнил условия кода " + code.getCode() +
                ". Ютубер " + code.getAuthor() +
                " получил " + code.getRewardMoney() + " монет");
    }
}