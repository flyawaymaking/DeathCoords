package com.flyaway.deathcoords;

import org.bukkit.plugin.java.JavaPlugin;

public class DeathCoords extends JavaPlugin {
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Инициализируем менеджеры
        this.configManager = new ConfigManager(this);

        // Регистрируем события
        getServer().getPluginManager().registerEvents(new PlayerListener(configManager), this);

        // Команда для перезагрузки конфига
        getCommand("deathcoords").setExecutor(new ReloadCommand(configManager));

        getLogger().info("DeathCoords включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DeathCoords выключен!");
    }
}
