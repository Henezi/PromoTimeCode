package com.yourname.promotimecode;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaytimeChecker extends BukkitRunnable {

    private int saveCounter = 0;
    private FileConfiguration config;

    private String getMsg(String key) {
        return config.getString("messages." + key, "&cСообщение не найдено: " + key);
    }

    private String getPrefix() {
        return config.getString("messages.prefix", "&6[Promo] &r");
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public void run() {
        config = PromoTimeCode.getInstance().getConfig();

        int checkInterval = config.getInt("settings.check-interval-seconds", 10);
        int saveInterval = config.getInt("settings.save-interval-minutes", 1);
        int savesPerSave = (saveInterval * 60) / checkInterval;

        saveCounter++;
        boolean shouldSave = (saveCounter >= savesPerSave);

        for (Player p : Bukkit.getOnlinePlayers()) {
            DataManager.PlayerData data = PromoTimeCode.getInstance().getDataManager().getPlayerData(p);

            if (data.activeCode == null || data.redeemed) continue;

            CodeManager.PromoCode code = PromoTimeCode.getInstance().getCodeManager().loadCode(data.activeCode);
            if (code == null) {
                data.activeCode = null;
                if (shouldSave) {
                    PromoTimeCode.getInstance().getDataManager().setPlayerData(p, data);
                }
                continue;
            }

            // ===== УВЕЛИЧИВАЕМ ТОЛЬКО КОГДА ИГРОК ОНЛАЙН =====
            data.playedSeconds += checkInterval;

            int elapsedMinutes = data.playedSeconds / 60;

            if (config.getBoolean("extra.log-actions", true) && elapsedMinutes > 0 && data.playedSeconds % 60 == 0) {
                Bukkit.getLogger().info("[PromoTimeCode] " + p.getName() + " прогресс: " + elapsedMinutes + "/" + code.requiredPlaytime + " минут");
            }

            if (elapsedMinutes >= code.requiredPlaytime && !data.redeemed) {
                Bukkit.getLogger().info("[PromoTimeCode] " + p.getName() + " отыграл " + elapsedMinutes + " минут! Выдаем награду...");

                giveReward(p, code);

                code.balance += code.rewardMoney;
                code.completedPlayers++;
                PromoTimeCode.getInstance().getCodeManager().saveCode(code);

                data.redeemed = true;
                PromoTimeCode.getInstance().getDataManager().setPlayerData(p, data);

                String prefix = getPrefix();
                String doneMsg = getMsg("code-done");
                p.sendMessage(color(prefix + doneMsg));

                List<String> rewardDetails = config.getStringList("messages.code-done-details");
                if (rewardDetails.isEmpty()) {
                    p.sendMessage(color("&7├─ &eГруппа: &f" + code.groupName + " &7на &f" + code.durationMinutes + " &7мин"));
                    p.sendMessage(color("&7└─ &eЮтубер получил награду: &f" + code.rewardMoney + " &7монет"));
                } else {
                    for (String detail : rewardDetails) {
                        detail = detail.replace("%group%", code.groupName)
                                .replace("%time%", String.valueOf(code.durationMinutes))
                                .replace("%reward%", String.valueOf(code.rewardMoney));
                        p.sendMessage(color(detail));
                    }
                }

                p.sendMessage(color("&aВам выдана группа &e" + code.groupName.toLowerCase() +
                        " &aна &e" + code.durationMinutes + " &aминут!"));

            } else if (shouldSave) {
                PromoTimeCode.getInstance().getDataManager().setPlayerData(p, data);
            }
        }

        if (shouldSave) {
            saveCounter = 0;
        }
    }

    private void giveReward(Player p, CodeManager.PromoCode code) {
        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(p.getUniqueId());

            if (user != null) {
                String groupName = code.groupName.toLowerCase();
                long durationMillis = TimeUnit.MINUTES.toMillis(code.durationMinutes);
                Instant expiry = Instant.now().plusMillis(durationMillis);

                user.data().add(Node.builder("group." + groupName)
                        .expiry(expiry)
                        .build());

                api.getUserManager().saveUser(user);

                if (config.getBoolean("extra.log-actions", true)) {
                    Bukkit.getLogger().info("[PromoTimeCode] Игроку " + p.getName() +
                            " выдана группа " + groupName + " на " + code.durationMinutes + " минут");
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[PromoTimeCode] Ошибка при выдаче награды: " + e.getMessage());
            e.printStackTrace();
        }
    }
}