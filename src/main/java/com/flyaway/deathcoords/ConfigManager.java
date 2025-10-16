package com.flyaway.deathcoords;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ConfigManager {
    private final DeathCoords plugin;
    private FileConfiguration config;
    private final Random random = new Random();
    private final Map<String, List<String>> deathMessages = new HashMap<>();

    public ConfigManager(DeathCoords plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        loadDeathMessages();
    }

    private void loadDeathMessages() {
        deathMessages.clear();
        ConfigurationSection deathSection = config.getConfigurationSection("death-messages");

        if (deathSection != null) {
            for (String key : deathSection.getKeys(false)) {
                List<String> messages = deathSection.getStringList(key);
                deathMessages.put(key, messages);
            }
        }

        plugin.getLogger().info("Загружено " + deathMessages.size() + " типов сообщений о смерти");
    }

    // ПРОВЕРКА ЕСТЬ ЛИ СООБЩЕНИЕ ДЛЯ ДАННОГО ТИПА СМЕРТИ
    public boolean hasDeathMessage(String deathType) {
        return deathMessages.containsKey(deathType) && !deathMessages.get(deathType).isEmpty();
    }

    public String getRandomDeathMessage(String deathType, String playerName, String killerName) {
        List<String> messages = deathMessages.get(deathType);

        if (messages == null || messages.isEmpty()) {
            return playerName + " погиб";
        }

        String message = messages.get(random.nextInt(messages.size()));
        message = message.replace("{player}", playerName);

        if (killerName != null) {
            message = message.replace("{killer}", killerName);
        }

        return message;
    }

    public boolean showDeathCoordinates() {
        return config.getBoolean("settings.show-death-coordinates", true);
    }

    public boolean showBackButton() {
        return config.getBoolean("settings.show-back-button", true);
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadDeathMessages();
    }
}
