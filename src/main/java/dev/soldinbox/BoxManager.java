package dev.soldinbox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        for (String key : config.getConfigurationSection("boxes").getKeys(false)) {
            Map<String, Object> map = (Map<String, Object>) config.getConfigurationSection("boxes." + key).getValues(false);
            String world = (String) map.get("world");
            int x = (int) map.get("x");
            int y = (int) map.get("y");
            int z = (int) map.get("z");
            Location loc = new Location(Bukkit.getWorld(world), x, y, z);
            boxes.put(Integer.parseInt(key), new BoxData(Integer.parseInt(key), loc));
        }
    }

    public void spawnBox(int id) {
        BoxData data = boxes.get(id);
        if (data != null) data.spawn();
    }

    public void spawnAll() {
        boxes.values().forEach(BoxData::spawn);
    }

    public void openEditor(Player player, int id) {
        BoxData data = boxes.get(id);
        if (data != null) data.openEditor(player);
    }
}
