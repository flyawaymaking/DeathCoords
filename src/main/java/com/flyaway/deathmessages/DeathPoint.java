package com.flyaway.deathmessages;

import org.bukkit.Location;

public class DeathPoint {

    private final Location location;
    private final long createdAt;

    public DeathPoint(Location location) {
        this.location = location.clone();
        this.createdAt = System.currentTimeMillis();
    }

    public Location getLocation() {
        return location.clone();
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
