package dev.soldinbox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class BoxManager {
    private final SoldInBox plugin;
    private final Map<Integer, BoxData> boxes = new HashMap<>();

    public BoxManager(SoldInBox plugin) {
        this.plugin = plugin;
        loadFromConfig();
    }

    public void loadFromConfig() {
        boxes.clear();
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("boxes");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                ConfigurationSection b = sec.getConfigurationSection(key);
                String world = b.getString("world", "world");
                double x = b.getDouble("x");
                double y = b.getDouble("y");
                double z = b.getDouble("z");
                int respawn = b.getInt("respawn_seconds", cfg.getInt("settings.default_respawn_seconds", 300));
                List<LootEntry> loot = new ArrayList<>();
                if (b.isList("loot")) {
                    for (Map<?,?> m : b.getMapList("loot")) {
                        Map<String,Object> map = (Map<String,Object>) m;
                        String mat = (String) map.getOrDefault("material", "STONE");
                        int amount = ((Number) map.getOrDefault("amount", 1)).intValue();
                        int chance = ((Number) map.getOrDefault("chance", 10)).intValue();
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

    public void saveToConfig() {
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("boxes", null);
        for (Map.Entry<Integer, BoxData> e : boxes.entrySet()) {
            int id = e.getKey();
            BoxData b = e.getValue();
            String path = "boxes." + id;
            cfg.set(path + ".world", b.getLocation().getWorld().getName());
            cfg.set(path + ".x", b.getLocation().getX());
            cfg.set(path + ".y", b.getLocation().getY());
            cfg.set(path + ".z", b.getLocation().getZ());
            cfg.set(path + ".respawn_seconds", b.getRespawnSeconds());
            List<Map<String,Object>> loot = b.getLoot().stream().map(le -> {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("material", le.getMaterialName());
                m.put("amount", le.getAmount());
                m.put("chance", le.getChance());
                return m;
            }).collect(Collectors.toList());
            cfg.set(path + ".loot", loot);
        }
        plugin.saveConfig();
    }

    public Collection<BoxData> getBoxes() { return boxes.values(); }
    public BoxData getBox(int id) { return boxes.get(id); }

    public int createBoxAt(org.bukkit.Location loc) {
        int id = boxes.keySet().stream().mapToInt(i->i).max().orElse(0) + 1;
        BoxData b = new BoxData(id, loc, plugin.getConfig().getInt("settings.default_respawn_seconds",300), new ArrayList<>());
        boxes.put(id, b);
        saveToConfig();
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
        saveToConfig();
        b.spawn();
    }

    public void resetAll() {
        boxes.values().forEach(b -> {
            b.clearBlock();
            b.setReady(true);
        });
        saveToConfig();
        spawnAll();
    }

    public void openEditor(org.bukkit.entity.Player p, int id) {
        BoxData b = boxes.get(id);
        if (b != null) b.openEditor(p);
        else p.sendMessage(plugin.getConfig().getString("messages.not_found"));
    }

    public SoldInBox getPlugin() { return plugin; }
}
