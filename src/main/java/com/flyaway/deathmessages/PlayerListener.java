package com.flyaway.deathmessages;

import net.kyori.adventure.text.minimessage.MiniMessage;
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

import java.util.HashMap;
import java.util.Map;

public class PlayerListener implements Listener {
    private final ConfigManager configManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PlayerListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location loc = player.getLocation();

        String deathType = determineDeathType(event);

        if (deathType != null && configManager.hasDeathMessage(deathType)) {
            event.deathMessage(null);
            String killerName = getKillerName(event);

            Component deathBroadcast = configManager.getRandomDeathMessage(deathType, player.getName(), killerName);
            Bukkit.broadcast(deathBroadcast);
        }

        if (configManager.showDeathCoordinates()) {
            sendPersonalDeathMessage(player, loc);
        }
    }

    private String determineDeathType(PlayerDeathEvent event) {
        Player player = event.getEntity();
        EntityDamageEvent lastDamage = player.getLastDamageCause();

        if (lastDamage == null) {
            return null;
        }

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

        return switch (cause) {
            case FIRE, FIRE_TICK, LAVA -> "fire";
            case MAGIC, POISON -> "magic";
            case CONTACT -> "cactus";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "explosion";
            default -> cause.name().toLowerCase();
        };
    }

    private boolean isKilledByPlayer(Player player) {
        // Прямой убийца-игрок
        if (player.getKiller() != null) {
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
        // Если это живая сущность (моб)
        if (attacker instanceof LivingEntity livingAttacker) {
            return getMobTypeFromEntity(livingAttacker);
        }
        // Если это снаряд - определяем стрелка
        if (attacker instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity livingShooter) {
                return getMobTypeFromEntity(livingShooter);
            }
            return null;
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

    private void sendPersonalDeathMessage(Player player, Location loc) {
        Map<String, String> coordsPlaceholders = new HashMap<>();
        coordsPlaceholders.put("x", String.valueOf(loc.getBlockX()));
        coordsPlaceholders.put("y", String.valueOf(loc.getBlockY()));
        coordsPlaceholders.put("z", String.valueOf(loc.getBlockZ()));

        String personalMessage = configManager.getMessage("personal-message", coordsPlaceholders);

        if (configManager.showBackButton() && hasBackOnDeathPermission(player)) {
            String backButton = configManager.getMessage("back-button", null);
            String backHover = configManager.getMessage("back-hover", null);
            String button = createButton(backButton, backHover);
            personalMessage = personalMessage + "<newline>" + button;
        }
        configManager.sendRawMessage(player, personalMessage);
    }

    private boolean hasBackOnDeathPermission(Player player) {
        return player.hasPermission("essentials.back.ondeath");
    }

    private String createButton(String button, String hover) {
        Component component = Component.text()
                .append(miniMessage.deserialize(button))
                .clickEvent(ClickEvent.runCommand("/back"))
                .hoverEvent(miniMessage.deserialize(hover)).build();
        return miniMessage.serialize(component);
    }
}
