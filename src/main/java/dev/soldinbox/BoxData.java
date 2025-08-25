package dev.soldinbox;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Shulker;

public class BoxData {
    private final int id;
    private final Location location;
    private Shulker shulker;

    public BoxData(int id, Location location) {
        this.id = id;
        this.location = location;
    }

    public void spawn() {
        if (location.getWorld() != null) {
            shulker = (Shulker) location.getWorld().spawnEntity(location, EntityType.SHULKER);
            shulker.setCustomName("§aLoot Box #" + id);
        }
    }
}
