package dev.soldinbox;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;

public class BoxData {
    private final int id;
    private Location location;
    private List<LootEntry> loot;

    public BoxData(int id, Location location, List<LootEntry> loot) {
        this.id = id;
        this.location = location;
        this.loot = loot;
    }

    public int getId() { return id; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public List<LootEntry> getLoot() { return loot; }
    public void setLoot(List<LootEntry> loot) { this.loot = loot; }

    // GUI holder marker
    public static class EditorHolder {}
}
