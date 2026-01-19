package com.flyaway.deathmessages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
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
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerListener implements Listener {

    private static final long DEATH_TTL = 10 * 60 * 1000;

    private final DeathMessages plugin;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final Map<UUID, Map<String, DeathPoint>> deathPoints = new HashMap<>();
    private BukkitTask cleanupTask;

    public PlayerListener(DeathMessages plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        startCleanupTask();
    }

    private void startCleanupTask() {
        this.cleanupTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::cleanupAllExpiredDeaths,
                20L * 60,
                20L * 60
        );
    }

    public void onDisable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        deathPoints.clear();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location loc = player.getLocation().clone();

        String deathType = determineDeathType(event);

        if (deathType != null && configManager.hasDeathMessage(deathType)) {
            event.deathMessage(null);
            Player killer = getKiller(event);

            Component broadcast = configManager.getRandomDeathMessage(
                    deathType,
                    player.getName(),
                    killer
            );
            Bukkit.broadcast(broadcast);
        }

        if (configManager.showDeathCoordinates()) {
            sendPersonalDeathMessage(player, loc);
        }
    }

    private String determineDeathType(PlayerDeathEvent event) {
        Player player = event.getEntity();
        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage == null) return null;

        if (isKilledByPlayer(player)) {
            return "player";
        }

        if (lastDamage instanceof EntityDamageByEntityEvent entityEvent) {
            Entity damager = entityEvent.getDamager();
            String mob = getMobDeathType(damager);
            if (mob != null) return mob;
        }

        return switch (lastDamage.getCause()) {
            case FIRE, FIRE_TICK, LAVA -> "fire";
            case MAGIC, POISON -> "magic";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "explosion";
            default -> lastDamage.getCause().name().toLowerCase();
        };
    }

    private boolean isKilledByPlayer(Player player) {
        if (player.getKiller() != null) return true;

        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent entityEvent) {
            if (entityEvent.getDamager() instanceof Projectile projectile) {
                ProjectileSource shooter = projectile.getShooter();
                return shooter instanceof Player;
            }
        }
        return false;
    }

    private String getMobDeathType(Entity attacker) {
        if (attacker instanceof LivingEntity livingAttacker) {
            return livingAttacker.getType().name().toLowerCase();
        }
        if (attacker instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity le) {
                return le.getType().name().toLowerCase();
            }
        }
        return null;
    }

    private Player getKiller(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) return killer;

        EntityDamageEvent last = event.getEntity().getLastDamageCause();
        if (last instanceof EntityDamageByEntityEvent e) {
            if (e.getDamager() instanceof Projectile p) {
                ProjectileSource shooter = p.getShooter();
                if (shooter instanceof Player pl) return pl;
            }
        }
        return null;
    }

    private void sendPersonalDeathMessage(Player player, Location loc) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("x", String.valueOf(loc.getBlockX()));
        placeholders.put("y", String.valueOf(loc.getBlockY()));
        placeholders.put("z", String.valueOf(loc.getBlockZ()));

        String baseMessage = configManager.getMessage("personal-message", placeholders);

        if (configManager.showBackButton() && player.hasPermission("deathmessages.dback")) {
            String deathId = storeDeathPoint(player, loc);

            String backButton = configManager.getMessage("back-button", placeholders);
            String backHover = configManager.getMessage("back-hover", placeholders);

            Component button = miniMessage.deserialize(backButton)
                    .clickEvent(ClickEvent.runCommand("/deathback " + deathId));

            if (!backHover.isEmpty()) {
                button = button.hoverEvent(miniMessage.deserialize(backHover));
            }

            baseMessage += "<newline>" + miniMessage.serialize(button);
        }

        configManager.sendRawMessage(player, baseMessage);
    }

    private String storeDeathPoint(Player player, Location loc) {
        String deathId = generateDeathId();
        deathPoints
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(deathId, new DeathPoint(loc));
        return deathId;
    }

    public DeathPoint consumeDeathPoint(UUID playerId, String deathId) {
        Map<String, DeathPoint> points = deathPoints.get(playerId);
        if (points == null) return null;
        return points.get(deathId);
    }

    private void cleanupAllExpiredDeaths() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, Map<String, DeathPoint>>> playerIterator =
                deathPoints.entrySet().iterator();

        while (playerIterator.hasNext()) {
            Map.Entry<UUID, Map<String, DeathPoint>> entry = playerIterator.next();
            Map<String, DeathPoint> points = entry.getValue();

            points.entrySet().removeIf(e ->
                    now - e.getValue().getCreatedAt() > DEATH_TTL
            );

            if (points.isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    private String generateDeathId() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong());
    }
}
