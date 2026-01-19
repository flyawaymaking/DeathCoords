package com.flyaway.deathmessages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeathBackCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final PlayerListener playerListener;

    public DeathBackCommand(ConfigManager configManager, PlayerListener playerListener) {
        this.configManager = configManager;
        this.playerListener = playerListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!player.hasPermission("deathmessages.dback") || args.length != 1) {
            return true;
        }

        String deathId = args[0];

        DeathPoint point = playerListener.consumeDeathPoint(player.getUniqueId(), deathId);
        if (point == null) {
            configManager.sendMessage(player, "deathback.invalid");
            return true;
        }

        player.teleport(point.getLocation());
        configManager.sendMessage(player, "deathback.success");
        return true;
    }
}
