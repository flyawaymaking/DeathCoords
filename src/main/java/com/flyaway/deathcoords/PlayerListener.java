package com.flyaway.deathcoords;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class PlayerListener implements Listener {
    private final ConfigManager configManager;

    public PlayerListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location loc = player.getLocation();

        String deathType = determineDeathType(event);
        String killerName = getKillerName(event);

        // ЕСЛИ ЕСТЬ КАСТОМНОЕ СООБЩЕНИЕ ДЛЯ ЭТОГО ТИПА - ИСПОЛЬЗУЕМ ЕГО
        if (deathType != null && configManager.hasDeathMessage(deathType)) {
            // Отключаем стандартное сообщение
            event.deathMessage(null);

            // Создаем красивое сообщение для всех игроков
            Component deathBroadcast = createDeathBroadcast(player, deathType, killerName);
            Bukkit.broadcast(deathBroadcast);
        }

        // Персональное сообщение с координатами для умершего игрока
        if (configManager.showDeathCoordinates()) {
            sendPersonalDeathMessage(player, loc);
        }
    }

    // ОПРЕДЕЛЕНИЕ ТИПА СМЕРТИ
    private String determineDeathType(PlayerDeathEvent event) {
        Player player = event.getEntity();
        EntityDamageEvent lastDamage = player.getLastDamageCause();

        if (lastDamage == null) {
            return null;
        }

        // Проверяем убийцу-игрока
        if (player.getKiller() instanceof Player) {
            return "player";
        }

        // Проверяем причину повреждения
        EntityDamageEvent.DamageCause cause = lastDamage.getCause();

        switch (cause) {
            case ENTITY_ATTACK:
                if (lastDamage instanceof EntityDamageByEntityEvent) {
                    Entity attacker = ((EntityDamageByEntityEvent) lastDamage).getDamager();
                    return getMobDeathType(attacker);
                }
                return null;

            case FALL:
                return "fall";

            case FIRE:
            case FIRE_TICK:
            case LAVA:
                return "fire";

            case DROWNING:
                return "drown";

            case STARVATION:
                return "starvation";

            case MAGIC:
            case POISON:
            case WITHER:
                return "magic";

            case CONTACT:
                return "cactus";

            case SUFFOCATION:
                return "suffocation";

            case BLOCK_EXPLOSION:
            case ENTITY_EXPLOSION:
                return "explosion";

            case LIGHTNING:
                return "lightning";

            default:
                return null;
        }
    }

    // 🔴 ОПРЕДЕЛЕНИЕ ТИПА СМЕРТИ ОТ МОБОВ
    private String getMobDeathType(Entity attacker) {
        if (attacker instanceof Skeleton) return "skeleton";
        else if (attacker instanceof Zombie) return "zombie";
        else if (attacker instanceof Creeper) return "creeper";
        else if (attacker instanceof Enderman) return "enderman";
        else if (attacker instanceof Spider) return "spider";
        else if (attacker instanceof CaveSpider) return "spider";
        else if (attacker instanceof Blaze) return "blaze";
        else if (attacker instanceof Ghast) return "ghast";
        else if (attacker instanceof Slime) return "slime";
        else if (attacker instanceof MagmaCube) return "slime";
        else if (attacker instanceof Witch) return "witch";
        else if (attacker instanceof Guardian) return "guardian";
        else if (attacker instanceof Phantom) return "phantom";
        else return null; // ДЛЯ НЕИЗВЕСТНЫХ МОБОВ ВОЗВРАЩАЕМ NULL
    }

    // ПОЛУЧЕНИЕ ИМЕНИ УБИЙЦЫ
    private String getKillerName(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Если убийца - игрок
        if (player.getKiller() instanceof Player) {
            return player.getKiller().getName();
        }
        return null;
    }

    // СОЗДАНИЕ СООБЩЕНИЯ О СМЕРТИ
    private Component createDeathBroadcast(Player player, String deathType, String killerName) {
        String message = configManager.getRandomDeathMessage(deathType, player.getName(), killerName);
        return Component.text(message, NamedTextColor.GRAY);
    }

    // ПЕРСОНАЛЬНОЕ СООБЩЕНИЕ С КООРДИНАТАМИ
    private void sendPersonalDeathMessage(Player player, Location loc) {
        Component coordsMessage = Component.text()
                .append(Component.text("💀 Координаты смерти: ", NamedTextColor.RED))
                .append(Component.text("X: " + loc.getBlockX() + " ", NamedTextColor.YELLOW))
                .append(Component.text("Y: " + loc.getBlockY() + " ", NamedTextColor.YELLOW))
                .append(Component.text("Z: " + loc.getBlockZ(), NamedTextColor.YELLOW))
                .build();

        if (configManager.showBackButton() && hasBackOnDeathPermission(player)) {
            Component fullMessage = Component.text()
                    .append(coordsMessage)
                    .append(Component.newline())
                    .append(createBackButton())
                    .build();
            player.sendMessage(fullMessage);
        } else {
            player.sendMessage(coordsMessage);
        }
    }

    private boolean hasBackOnDeathPermission(Player player) {
        return player.hasPermission("essentials.back.ondeath");
    }

    private Component createBackButton() {
        return Component.text()
                .append(Component.text("[✨ ВЕРНУТЬСЯ]", NamedTextColor.GREEN, TextDecoration.BOLD))
                .clickEvent(ClickEvent.runCommand("/back"))
                .hoverEvent(Component.text("Нажмите чтобы вернуться к месту смерти", NamedTextColor.GRAY))
                .build();
    }
}
