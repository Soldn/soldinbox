package dev.soldinbox;

import org.bukkit.Location;

import java.util.*;

public class Box {
    private final int id;
    private final Location location;
    private int respawnSeconds;
    private List<LootEntry> loot;
    private boolean ready = true;
    private java.util.UUID entityId;

    public Box(int id, Location location, int respawnSeconds, java.util.List<LootEntry> loot) {
        this.id = id;
        this.location = location;
        this.respawnSeconds = respawnSeconds;
        this.loot = new ArrayList<>(loot);
    }

    public int getId() { return id; }
    public Location getLocation() { return location; }
    public int getRespawnSeconds() { return respawnSeconds; }
    public void setRespawnSeconds(int s) { this.respawnSeconds = s; }
    public List<LootEntry> getLoot() { return loot; }
    public void setLoot(List<LootEntry> list) { this.loot = new ArrayList<>(list); }
    public boolean isReady() { return ready; }
    public void setReady(boolean r) { this.ready = r; }
    public java.util.UUID getEntityId() { return entityId; }
    public void setEntityId(java.util.UUID id) { this.entityId = id; }

    public LootEntry findMatchingLoot(org.bukkit.inventory.ItemStack it) {
        for (LootEntry le : loot) {
            if (le.getItem().getType() == it.getType() &&
                le.getItem().getAmount() == it.getAmount()) {
                return le;
            }
        }
        return null;
    }
}
