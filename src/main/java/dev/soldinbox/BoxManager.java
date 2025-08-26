package dev.soldinbox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BoxManager {
    private final SoldInBox plugin;
    private final Map<Integer, BoxData> boxes = new HashMap<>();
    private final File file;

    public BoxManager(SoldInBox plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "boxes.yml");
        load();
    }

    public void load() {
        boxes.clear();
        plugin.getConfigManager().loadBoxes(file, boxes);
    }

    public void saveToFile() {
        plugin.getConfigManager().saveBoxes(file, boxes);
    }

    public BoxData getBox(int id) {
        return boxes.get(id);
    }

    public void addBox(BoxData box) {
        boxes.put(box.getId(), box);
        saveToFile();
    }

    public Collection<BoxData> getBoxes() {
        return boxes.values();
    }

    public void spawnBox(BoxData box) {
        Location loc = box.getLocation();
        if (loc == null) return;

        Block block = loc.getBlock();
        block.setType(Material.SHULKER_BOX);

        ShulkerBox shulker = (ShulkerBox) block.getState();
        shulker.getInventory().clear();

        for (LootEntry loot : box.getLoot()) {
            try {
                ItemStack item = new ItemStack(Material.valueOf(loot.getMaterial()), loot.getAmount());
                shulker.getInventory().addItem(item);
            } catch (Exception ignored) {}
        }
        shulker.update();
    }
}
