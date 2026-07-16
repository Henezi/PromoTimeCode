package com.kvassuk.promotimecode.managers;

import com.kvassuk.promotimecode.PromoTimeCode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupManager {

    private final PromoTimeCode plugin;
    private final File backupFolder;

    public BackupManager(PromoTimeCode plugin) {
        this.plugin = plugin;
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
    }

    /**
     * Создать полный бэкап всех важных данных
     * @return имя созданного бэкапа, или null при ошибке
     */
    public String createFullBackup() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String backupName = "backup_" + timestamp;
        File backupDir = new File(backupFolder, backupName);

        try {
            backupDir.mkdirs();
            plugin.getLogger().info("Создание бэкапа: " + backupName);

            // 1. Копируем playerdata
            File playerdataSrc = new File(plugin.getDataFolder(), "playerdata");
            if (playerdataSrc.exists() && playerdataSrc.isDirectory()) {
                copyFolder(playerdataSrc, new File(backupDir, "playerdata"));
                plugin.getLogger().info("  ✓ playerdata/ скопирован");
            }

            // 2. Копируем codes
            File codesSrc = new File(plugin.getDataFolder(), "codes");
            if (codesSrc.exists() && codesSrc.isDirectory()) {
                copyFolder(codesSrc, new File(backupDir, "codes"));
                plugin.getLogger().info("  ✓ codes/ скопирован");
            }

            // 3. Копируем ip_data.json
            File ipDataSrc = new File(plugin.getDataFolder(), "ip_data.json");
            if (ipDataSrc.exists()) {
                Files.copy(ipDataSrc.toPath(), new File(backupDir, "ip_data.json").toPath());
                plugin.getLogger().info("  ✓ ip_data.json скопирован");
            }

            // 4. Копируем конфиги
            copyConfigFile("config.yml", backupDir);
            copyConfigFile("antiabuse.yml", backupDir);
            copyConfigFile("rewards.yml", backupDir);

            // 5. Копируем языковые файлы (если есть)
            File langSrc = new File(plugin.getDataFolder(), "languages");
            if (langSrc.exists() && langSrc.isDirectory()) {
                copyFolder(langSrc, new File(backupDir, "languages"));
                plugin.getLogger().info("  ✓ languages/ скопирован");
            }

            plugin.getLogger().info("Бэкап " + backupName + " успешно создан!");
            return backupName;

        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка создания бэкапа: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Восстановить данные из бэкапа
     * @param backupName имя папки бэкапа
     * @return true если успешно
     */
    public boolean restoreBackup(String backupName) {
        File backupDir = new File(backupFolder, backupName);
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            plugin.getLogger().warning("Бэкап " + backupName + " не найден!");
            return false;
        }

        try {
            plugin.getLogger().info("Восстановление из бэкапа: " + backupName);

            // Останавливаем автосохранение
            plugin.getSaveManager().stopAutoSave();

            // Сохраняем текущие данные
            plugin.getSaveManager().saveAll();

            // 1. Восстанавливаем playerdata
            File playerdataBackup = new File(backupDir, "playerdata");
            if (playerdataBackup.exists() && playerdataBackup.isDirectory()) {
                File playerdataDest = new File(plugin.getDataFolder(), "playerdata");
                deleteFolder(playerdataDest);
                copyFolder(playerdataBackup, playerdataDest);
                plugin.getLogger().info("  ✓ playerdata/ восстановлен");
            }

            // 2. Восстанавливаем codes
            File codesBackup = new File(backupDir, "codes");
            if (codesBackup.exists() && codesBackup.isDirectory()) {
                File codesDest = new File(plugin.getDataFolder(), "codes");
                deleteFolder(codesDest);
                copyFolder(codesBackup, codesDest);
                plugin.getLogger().info("  ✓ codes/ восстановлен");
            }

            // 3. Восстанавливаем ip_data.json
            File ipDataBackup = new File(backupDir, "ip_data.json");
            if (ipDataBackup.exists()) {
                Files.copy(ipDataBackup.toPath(), new File(plugin.getDataFolder(), "ip_data.json").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("  ✓ ip_data.json восстановлен");
            }

            // 4. Восстанавливаем конфиги
            restoreConfigFile("config.yml", backupDir);
            restoreConfigFile("antiabuse.yml", backupDir);
            restoreConfigFile("rewards.yml", backupDir);

            // 5. Восстанавливаем языковые файлы
            File langBackup = new File(backupDir, "languages");
            if (langBackup.exists() && langBackup.isDirectory()) {
                File langDest = new File(plugin.getDataFolder(), "languages");
                deleteFolder(langDest);
                copyFolder(langBackup, langDest);
                plugin.getLogger().info("  ✓ languages/ восстановлен");
            }

            // Перезагружаем данные в памяти
            plugin.getConfigManager().reload();
            plugin.getLanguageManager().loadLanguage(plugin.getConfigManager().getLanguage());
            plugin.getDataManager().loadAll();
            plugin.getCodeManager().loadAll();
            plugin.getAntiAbuseManager().reload();

            // Возобновляем автосохранение
            plugin.getSaveManager().startAutoSave();

            plugin.getLogger().info("Восстановление из бэкапа " + backupName + " завершено!");
            return true;

        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка восстановления бэкапа: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String[] getBackupList() {
        File[] files = backupFolder.listFiles(File::isDirectory);
        if (files == null) return new String[0];

        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getName();
        }
        return names;
    }

    public String getLatestBackup() {
        String[] backups = getBackupList();
        if (backups.length == 0) return null;

        // Сортируем по имени (дата в имени, поэтому сортировка работает)
        java.util.Arrays.sort(backups);
        return backups[backups.length - 1];
    }

    // ============================================================
    //                    ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ============================================================

    private void copyFolder(File source, File destination) throws IOException {
        if (!source.exists()) return;
        if (!destination.exists()) destination.mkdirs();

        File[] files = source.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                copyFolder(file, new File(destination, file.getName()));
            } else {
                Files.copy(file.toPath(), new File(destination, file.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void deleteFolder(File folder) {
        if (!folder.exists()) return;

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }

    private void copyConfigFile(String fileName, File backupDir) throws IOException {
        File src = new File(plugin.getDataFolder(), fileName);
        if (src.exists()) {
            Files.copy(src.toPath(), new File(backupDir, fileName).toPath());
            plugin.getLogger().info("  ✓ " + fileName + " скопирован");
        }
    }

    private void restoreConfigFile(String fileName, File backupDir) throws IOException {
        File backup = new File(backupDir, fileName);
        if (backup.exists()) {
            File dest = new File(plugin.getDataFolder(), fileName);
            Files.copy(backup.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("  ✓ " + fileName + " восстановлен");
        }
    }

    public File getBackupFolder() {
        return backupFolder;
    }
}