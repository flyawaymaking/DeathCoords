package com.flyaway.deathcoords;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadCommand implements CommandExecutor {
    private final ConfigManager configManager;

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("deathcoords.reload")) {
                sender.sendMessage(Component.text("У вас нет прав для этой команды!", NamedTextColor.RED));
                return true;
            }

            configManager.reloadConfig();
            sender.sendMessage(Component.text("Конфиг DeathCoords перезагружен!", NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("Использование: /deathcoords reload", NamedTextColor.YELLOW));
        return true;
    }
}
