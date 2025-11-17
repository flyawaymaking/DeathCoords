package com.flyaway.deathmessages;

import org.bukkit.plugin.java.JavaPlugin;

public class DeathMessages extends JavaPlugin {

    @Override
    public void onEnable() {
        // Инициализируем менеджеры
        ConfigManager configManager = new ConfigManager(this);

        // Регистрируем события
        getServer().getPluginManager().registerEvents(new PlayerListener(configManager), this);

        // Команда для перезагрузки конфига
        getCommand("deathmessages").setExecutor(new ReloadCommand(configManager));

        getLogger().info("DeathMessages включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DeathMessages выключен!");
    }
}
