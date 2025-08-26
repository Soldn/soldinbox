package dev.soldinbox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class BoxManager {
    private final SoldInBox plugin;
    private final Map<Integer, BoxData> boxes = new HashMap<>();
    private final File boxesFile;
    private final YamlConfiguration boxesCfg;

    public BoxManager(SoldInBox plugin) {
        this.plugin = plugin;
        this.boxesFile = new File(plugin.getDataFolder(), "boxes.yml");
        this.boxesCfg = YamlConfiguration.loadConfiguration(boxesFile);
        loadFromConfig();
    }

    public void loadFromConfig() {
        boxes.clear();
        if (!boxesCfg.isConfigurationSection("boxes")) return;
        for (String key : boxesCfg.getConfigurationSection("boxes").getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                String world = boxesCfg.getString("boxes." + key + ".world", "world");
                double x = boxesCfg.getDouble("boxes." + key + ".x");
                double y = boxesCfg.getDouble("boxes." + key + ".y");
                double z = boxesCfg.getDouble("boxes." + key + ".z");
                int respawn = boxesCfg.getInt("boxes." + key + ".respawn_seconds", plugin.getConfig().getInt("settings.default_respawn_seconds",300));
                List<LootEntry> loot = new ArrayList<>();
                if (boxesCfg.isList("boxes." + key + ".loot")) {
                    for (Map<?,?> m : boxesCfg.getMapList("boxes." + key + ".loot")) {
                        Map<String,Object> map = (Map<String,Object>) m;
                        String mat = (String) map.getOrDefault("material", "STONE");
                        int amount = ((Number) map.getOrDefault("amount", 1)).intValue();
                        double chance = ((Number) map.getOrDefault("chance", 10.0)).doubleValue();
                        loot.add(new LootEntry(mat, amount, chance));
                    }
                }
                BoxData box = new BoxData(id, new Location(Bukkit.getWorld(world), x, y, z), respawn, loot);
                boxes.put(id, box);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to load box: " + key + " - " + ex.getMessage());
            }
        }
    }

    public void saveToFile() {
        try {
            boxesCfg.set("boxes", null);
            for (Map.Entry<Integer, BoxData> e : boxes.entrySet()) {
                int id = e.getKey();
                BoxData b = e.getValue();
                String path = "boxes." + id;
                boxesCfg.set(path + ".world", b.getLocation().getWorld().getName());
                boxesCfg.set(path + ".x", b.getLocation().getX());
                boxesCfg.set(path + ".y", b.getLocation().getY());
                boxesCfg.set(path + ".z", b.getLocation().getZ());
                boxesCfg.set(path + ".respawn_seconds", b.getRespawnSeconds());
                List<Map<String,Object>> loot = b.getLoot().stream().map(le -> {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("material", le.getMaterialName());
                    m.put("amount", le.getAmount());
                    m.put("chance", le.getChance());
                    return m;
                }).collect(Collectors.toList());
                boxesCfg.set(path + ".loot", loot);
            }
            boxesCfg.save(boxesFile);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save boxes.yml: " + ex.getMessage());
        }
    }

    public Collection<BoxData> getBoxes() { return boxes.values(); }
    public BoxData getBox(int id) { return boxes.get(id); }

    public int createBoxAt(org.bukkit.Location loc) {
        int id = boxes.keySet().stream().mapToInt(i->i).max().orElse(0) + 1;
        BoxData b = new BoxData(id, loc, plugin.getConfig().getInt("settings.default_respawn_seconds",300), new ArrayList<>());
        boxes.put(id, b);
        saveToFile();
        b.spawn();
        return id;
    }

    public void spawnAll() {
        boxes.values().forEach(BoxData::spawn);
    }

    public void spawnBox(int id) {
        BoxData b = boxes.get(id);
        if (b != null) b.spawn();
    }

    public void resetBox(int id) {
        BoxData b = boxes.get(id);
        if (b == null) return;
        b.clearBlock();
        b.setReady(true);
        saveToFile();
        b.spawn();
    }

    public void resetAll() {
        boxes.values().forEach(b -> {
            b.clearBlock();
            b.setReady(true);
        });
        saveToFile();
        spawnAll();
    }

    public void openEditor(org.bukkit.entity.Player p, int id) {
        BoxData b = boxes.get(id);
        if (b != null) b.openEditor(p);
        else p.sendMessage(plugin.getConfig().getString("messages.not_found"));
    }

    public SoldInBox getPlugin() { return plugin; }
}
