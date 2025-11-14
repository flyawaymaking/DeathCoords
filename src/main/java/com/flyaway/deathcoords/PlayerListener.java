package com.flyaway.deathcoords;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

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

        // Проверяем убийцу-игрока (включая снаряды от игроков)
        if (isKilledByPlayer(player)) {
            return "player";
        }

        if (lastDamage instanceof EntityDamageByEntityEvent entityEvent) {
            Entity damager = entityEvent.getDamager();
            String mobType = getMobDeathType(damager);
            if (mobType != null) {
                return mobType;
            }
        }

        EntityDamageEvent.DamageCause cause = lastDamage.getCause();

        switch (cause) {
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

    private boolean isKilledByPlayer(Player player) {
        // Прямой убийца-игрок
        if (player.getKiller() instanceof Player) {
            return true;
        }

        // Снаряд от игрока
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent entityEvent) {
            Entity damager = entityEvent.getDamager();
            if (damager instanceof Projectile projectile) {
                ProjectileSource shooter = projectile.getShooter();
                return shooter instanceof Player;
            }
        }

        return false;
    }

    private String getMobDeathType(Entity attacker) {
        // Если это снаряд - определяем стрелка
        if (attacker instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity livingShooter) {
                return getMobTypeFromEntity(livingShooter);
            }
            return null;
        }

        // Если это живая сущность (моб)
        if (attacker instanceof LivingEntity livingAttacker) {
            return getMobTypeFromEntity(livingAttacker);
        }

        return null;
    }

    private String getMobTypeFromEntity(LivingEntity entity) {
        // Используем встроенное имя типа сущности в нижнем регистре
        return entity.getType().name().toLowerCase();
    }

    // ПОЛУЧЕНИЕ ИМЕНИ УБИЙЦЫ
    private String getKillerName(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Прямой убийца-игрок
        Player killer = player.getKiller();
        if (killer != null) {
            return killer.getName();
        }

        // Убийца через снаряд
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent entityEvent) {
            Entity damager = entityEvent.getDamager();
            if (damager instanceof Projectile projectile) {
                ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof Player playerShooter) {
                    return playerShooter.getName();
                }
            }
        }

        return null;
    }

    private Component createDeathBroadcast(Player player, String deathType, String killerName) {
        String message = configManager.getRandomDeathMessage(deathType, player.getName(), killerName);
        return Component.text(message, NamedTextColor.GRAY);
    }

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
