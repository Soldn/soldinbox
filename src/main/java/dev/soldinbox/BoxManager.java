package dev.soldinbox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Shulker;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class BoxManager {
    private final SoldInBox plugin;
    private final Map<Integer, BoxData> boxes = new HashMap<>();

    public BoxManager(SoldInBox plugin) {
        this.plugin = plugin;
        loadBoxes();
    }

    public void loadBoxes() {
        boxes.clear();
        FileConfiguration config = plugin.getConfig();
        List<Map<String, Object>> list = config.getMapList("boxes")
                .stream()
                .map(m -> (Map<String, Object>) m)
                .collect(Collectors.toList());

        int id = 1;
        for (Map<String, Object> map : list) {
            String worldName = (String) map.get("world");
            int x = (int) map.get("x");
            int y = (int) map.get("y");
            int z = (int) map.get("z");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Location loc = new Location(world, x, y, z);
                boxes.put(id, new BoxData(id, loc));
            }
            id++;
        }
    }

    public void spawnBox(int id) {
        BoxData data = boxes.get(id);
        if (data == null) return;
        data.spawn();
    }

    public void spawnAll() {
        boxes.values().forEach(BoxData::spawn);
    }
}
