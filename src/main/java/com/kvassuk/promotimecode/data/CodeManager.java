package com.kvassuk.promotimecode.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kvassuk.promotimecode.PromoTimeCode;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CodeManager {

    private final PromoTimeCode plugin;
    private final Gson gson;
    private final File codesFolder;
    private final Map<String, PromoCode> codeCache = new ConcurrentHashMap<>();

    public CodeManager(PromoTimeCode plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .create();
        this.codesFolder = new File(plugin.getDataFolder(), "codes");
    }

    public void loadAll() {
        if (!codesFolder.exists()) {
            codesFolder.mkdirs();
            return;
        }

        File[] files = codesFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        int loaded = 0;
        for (File file : files) {
            String codeName = file.getName().replace(".json", "");
            PromoCode code = loadCode(codeName);
            if (code != null) {
                codeCache.put(codeName.toUpperCase(), code);
                loaded++;
            }
        }

        plugin.getLogger().info("Загружено промокодов: " + loaded);
    }

    public PromoCode loadCode(String code) {
        File file = new File(codesFolder, code.toUpperCase() + ".json");
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, PromoCode.class);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось загрузить код " + code + ": " + e.getMessage());
            return null;
        }
    }

    public void saveCode(String code) {
        PromoCode promoCode = codeCache.get(code.toUpperCase());
        if (promoCode == null || !promoCode.isDirty()) return;

        File file = new File(codesFolder, code.toUpperCase() + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(promoCode, writer);
            promoCode.markClean();
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить код " + code + ": " + e.getMessage());
        }
    }

    public void saveAllDirty() {
        for (String code : codeCache.keySet()) {
            saveCode(code);
        }
    }

    public boolean createCode(String code, String group, int duration, double reward, int requiredTime, String author) {
        String upperCode = code.toUpperCase();
        if (codeCache.containsKey(upperCode)) {
            return false;
        }

        PromoCode promoCode = new PromoCode(upperCode);
        promoCode.setAuthor(author);
        promoCode.setGroupName(group);
        promoCode.setDurationMinutes(duration);
        promoCode.setRewardMoney(reward);
        promoCode.setRequiredPlaytime(requiredTime);

        codeCache.put(upperCode, promoCode);
        saveCode(upperCode);
        return true;
    }

    public PromoCode getCode(String code) {
        return codeCache.get(code.toUpperCase());
    }

    public boolean deleteCode(String code) {
        String upperCode = code.toUpperCase();
        PromoCode removed = codeCache.remove(upperCode);
        if (removed != null) {
            File file = new File(codesFolder, upperCode + ".json");
            if (file.exists()) {
                file.delete();
            }
            return true;
        }
        return false;
    }

    public boolean codeExists(String code) {
        return codeCache.containsKey(code.toUpperCase());
    }

    public PromoCode findCodeByAuthor(String author) {
        for (PromoCode code : codeCache.values()) {
            if (code.getAuthor().equalsIgnoreCase(author)) {
                return code;
            }
        }
        return null;
    }

    public Map<String, PromoCode> getCodes() {
        return codeCache;
    }

    public int getCodeCount() {
        return codeCache.size();
    }
}