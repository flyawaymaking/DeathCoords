package com.flyaway.deathmessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ConfigManager {
    private final DeathMessages plugin;
    private FileConfiguration config;
    private final Random random = new Random();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, List<String>> deathMessages = new HashMap<>();
    private String prefix;
    private String deathPrefix;

    public ConfigManager(DeathMessages plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        this.prefix = plugin.getConfig().getString("messages.prefix", "<gray>[<red>DeathMessages</red>]</gray>");
        this.deathPrefix = plugin.getConfig().getString("death-messages.prefix", "<gray>");
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

    public @NotNull String getMessage(String key, Map<String, String> placeholders) {
        String message = plugin.getConfig().getString("messages." + key, "<red>message." + key + " not-found");
        return formatMessage(message, placeholders);
    }

    private @NotNull String formatMessage(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return "";
        message = message.replaceAll("\\s+$", "");

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        sendRawMessage(sender, message);
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, null);
    }

    public void sendRawMessage(CommandSender sender, String message) {
        if (message.isEmpty()) return;
        sender.sendMessage(miniMessage.deserialize(prefix + " » " + message));
    }

    public boolean hasDeathMessage(String deathType) {
        return deathMessages.containsKey(deathType) && !deathMessages.get(deathType).isEmpty();
    }

    public Component getRandomDeathMessage(String deathType, String playerName, Player killer) {
        List<String> messages = deathMessages.get(deathType);

        if (messages == null || messages.isEmpty()) {
            return miniMessage.deserialize(deathPrefix + playerName + " died");
        }

        String message = messages.get(random.nextInt(messages.size()));
        message = message.replace("{player}", playerName);

        if (killer != null) {
            String healthStr = String.format("%.1f", killer.getHealth());

            message = message.replace("{killer}", killer.getName()).replace("{killer_health}", healthStr);
        }

        return miniMessage.deserialize(deathPrefix + message);
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
        loadConfig();
    }
}
