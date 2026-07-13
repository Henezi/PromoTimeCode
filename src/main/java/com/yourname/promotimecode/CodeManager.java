package com.yourname.promotimecode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CodeManager {

    private final File codesFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public CodeManager() {
        codesFolder = new File(PromoTimeCode.getInstance().getDataFolder(), "codes");
        codesFolder.mkdirs();
    }

    public boolean createCode(String code, String group, int duration, double reward, int requiredTime, String author) {
        File codeFile = new File(codesFolder, code + ".json");
        if (codeFile.exists()) return false;

        PromoCode promoCode = new PromoCode();
        promoCode.code = code;
        promoCode.groupName = group.toLowerCase();
        promoCode.durationMinutes = duration;
        promoCode.rewardMoney = reward;
        promoCode.requiredPlaytime = requiredTime;
        promoCode.author = author;
        promoCode.players = new ArrayList<>();
        promoCode.balance = 0.0;
        promoCode.completedPlayers = 0;

        try (FileWriter writer = new FileWriter(codeFile)) {
            gson.toJson(promoCode, writer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public PromoCode loadCode(String code) {
        File codeFile = new File(codesFolder, code + ".json");
        if (!codeFile.exists()) return null;
        try (FileReader reader = new FileReader(codeFile)) {
            return gson.fromJson(reader, PromoCode.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveCode(PromoCode code) {
        File codeFile = new File(codesFolder, code.code + ".json");
        try (FileWriter writer = new FileWriter(codeFile)) {
            gson.toJson(code, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasActiveCode(Player p) {
        DataManager.PlayerData data = PromoTimeCode.getInstance().getDataManager().getPlayerData(p);
        return data.activeCode != null;
    }

    public boolean activateCode(Player p, String code) {
        PromoCode promoCode = loadCode(code);
        if (promoCode == null) return false;
        if (hasActiveCode(p)) return false;

        DataManager.PlayerData data = PromoTimeCode.getInstance().getDataManager().getPlayerData(p);
        data.activeCode = code;
        data.playedSeconds = 0;  // ← ОБНУЛЯЕМ при активации
        data.redeemed = false;
        PromoTimeCode.getInstance().getDataManager().setPlayerData(p, data);

        if (!promoCode.players.contains(p.getUniqueId().toString())) {
            promoCode.players.add(p.getUniqueId().toString());
            saveCode(promoCode);
        }
        return true;
    }

    // ===== НОВЫЙ МЕТОД: Получить все коды =====
    public List<PromoCode> getAllCodes() {
        List<PromoCode> codes = new ArrayList<>();
        File[] files = codesFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return codes;

        for (File file : files) {
            String codeName = file.getName().replace(".json", "");
            PromoCode code = loadCode(codeName);
            if (code != null) {
                codes.add(code);
            }
        }
        return codes;
    }

    // ===== НОВЫЙ МЕТОД: Найти код по автору =====
    public PromoCode findCodeByAuthor(String author) {
        for (PromoCode code : getAllCodes()) {
            if (code.author.equalsIgnoreCase(author)) {
                return code;
            }
        }
        return null;
    }

    // ===== НОВЫЙ МЕТОД: Удалить код =====
    public boolean deleteCode(String code) {
        File codeFile = new File(codesFolder, code + ".json");
        return codeFile.delete();
    }

    public static class PromoCode {
        public String code;
        public String groupName;
        public int durationMinutes;
        public double rewardMoney;
        public int requiredPlaytime;
        public String author;
        public List<String> players;
        public double balance;
        public int completedPlayers;
    }
}