package com.flyaway.deathmessages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    private final ConfigManager configManager;

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("deathmessages.reload")) {
                configManager.sendMessage(sender, "reload.no-permission");
                return true;
            }

            configManager.reloadConfig();
            configManager.sendMessage(sender, "reload.success");
            return true;
        }

        configManager.sendMessage(sender, "reload.usage");
        return true;
    }
}
