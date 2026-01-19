package com.flyaway.deathmessages;

import org.bukkit.plugin.java.JavaPlugin;

public class DeathMessages extends JavaPlugin {

    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        ConfigManager configManager = new ConfigManager(this);
        playerListener = new PlayerListener(this, configManager);

        getServer().getPluginManager().registerEvents(playerListener, this);
        getCommand("deathmessages").setExecutor(new ReloadCommand(configManager));
        getCommand("deathback").setExecutor(new DeathBackCommand(configManager, playerListener));
    }

    @Override
    public void onDisable() {
        if (playerListener != null) {
            playerListener.onDisable();
        }
        getLogger().info("DeathMessages выключен!");
    }
}
