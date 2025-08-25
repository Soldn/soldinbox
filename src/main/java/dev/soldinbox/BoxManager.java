package dev.soldinbox;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Shulker;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.NumberConversions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BoxManager {
    private final SoldInBox plugin;
    private final Map<Integer, Box> boxes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> entityToBox = new ConcurrentHashMap<>();

    public BoxManager(SoldInBox plugin) { this.plugin = plugin; }

    public Map<Integer, Box> getBoxes() { return boxes; }
    public Box getBox(int id) { return boxes.get(id); }

    public void loadFromConfig() {
        boxes.clear();
        entityToBox.clear();
        ConfigurationSection cs = plugin.getConfig().getConfigurationSection("boxes");
        if (cs == null) return;
        for (String key : cs.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                ConfigurationSection bcs = cs.getConfigurationSection(key);
                String world = bcs.getString("world", "world");
                World w = Bukkit.getWorld(world);
                if (w == null) continue;
                double x = bcs.getDouble("x"), y = bcs.getDouble("y"), z = bcs.getDouble("z");
                int respawn = bcs.getInt("respawn-seconds", plugin.getConfig().getInt("settings.default-respawn-seconds", 300));
                List<LootEntry> loot = new ArrayList<>();
                if (bcs.isList("loot")) {
                    for (Map<?,?> m : bcs.getMapList("loot")) {
                        String mat = String.valueOf(m.getOrDefault("material", "STONE"));
                        int chance = NumberConversions.toInt(m.getOrDefault("chance", 10));
                        int amount = NumberConversions.toInt(m.getOrDefault("amount", 1));
                        ItemStack it;
                        try {
                            it = new ItemStack(Material.valueOf(mat.toUpperCase()), Math.max(1, amount));
                        } catch (IllegalArgumentException ex) {
                            it = new ItemStack(Material.STONE, Math.max(1, amount));
                        }
                        loot.add(new LootEntry(it, Math.max(0, Math.min(100, chance))));
                    }
                }
                Box bx = new Box(id, new Location(w, x, y, z), respawn, loot);
                if (bcs.isSet("ready")) bx.setReady(bcs.getBoolean("ready", true));
                if (bcs.isSet("entity")) {
                    try {
                        UUID uuid = UUID.fromString(bcs.getString("entity"));
                        bx.setEntityId(uuid);
                    } catch (Exception ignored) {}
                }
                boxes.put(id, bx);
            } catch (Exception ignored) {}
        }
    }

    public void saveToConfig() {
        plugin.getConfig().set("boxes", null);
        for (Map.Entry<Integer, Box> e : boxes.entrySet()) {
            int id = e.getKey();
            Box b = e.getValue();
            String path = "boxes." + id + ".";
            plugin.getConfig().set(path + "world", b.getLocation().getWorld().getName());
            plugin.getConfig().set(path + "x", b.getLocation().getX());
            plugin.getConfig().set(path + "y", b.getLocation().getY());
            plugin.getConfig().set(path + "z", b.getLocation().getZ());
            plugin.getConfig().set(path + "respawn-seconds", b.getRespawnSeconds());
            plugin.getConfig().set(path + "ready", b.isReady());
            plugin.getConfig().set(path + "entity", b.getEntityId() == null ? null : b.getEntityId().toString());
            List<Map<String,Object>> loot = new ArrayList<>();
            for (LootEntry le : b.getLoot()) {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("material", le.getItem().getType().name());
                m.put("amount", le.getItem().getAmount());
                m.put("chance", le.getChance());
                loot.add(m);
            }
            plugin.getConfig().set(path + "loot", loot);
        }
        plugin.saveConfig();
    }

    public void spawnAllIfReady() {
        for (Box b : boxes.values()) {
            if (b.isReady()) spawnShulker(b);
            else scheduleRespawn(b, 1);
        }
    }

    public void shutdown() {
        // Despawn managed shulkers
        for (Box b : boxes.values()) {
            if (b.getEntityId() != null) {
                Entity e = findEntity(b.getEntityId());
                if (e != null) e.remove();
            }
        }
        boxes.clear();
        entityToBox.clear();
    }

    public int createBoxAt(Location loc) {
        int id = boxes.keySet().stream().mapToInt(i -> i).max().orElse(0) + 1;
        Box b = new Box(id, loc.clone(), plugin.getConfig().getInt("settings.default-respawn-seconds", 300), new ArrayList<>());
        boxes.put(id, b);
        saveToConfig();
        spawnShulker(b);
        return id;
    }

    public boolean resetBox(int id) {
        Box b = boxes.get(id);
        if (b == null) return false;
        consumeBoxCancel(b); // ensure despawn
        b.setReady(true);
        spawnShulker(b);
        saveToConfig();
        return true;
    }

    public void resetAll() {
        for (int id : new ArrayList<>(boxes.keySet())) resetBox(id);
    }

    public Box getByEntity(UUID uuid) {
        Integer id = entityToBox.get(uuid);
        if (id == null) return null;
        return boxes.get(id);
    }

    public void fillInventoryWithLoot(Box b, Inventory inv) {
        Random rnd = new Random();
        List<ItemStack> selected = new ArrayList<>();
        for (LootEntry le : b.getLoot()) {
            if (rnd.nextInt(100) < le.getChance()) {
                selected.add(le.getItem().clone());
            }
        }
        // place items
        int i = 0;
        for (ItemStack it : selected) {
            if (i >= inv.getSize()) break;
            inv.setItem(i++, it);
        }
    }

    public void consumeBox(Box b) {
        // Despawn shulker and schedule respawn
        consumeBoxCancel(b);
        b.setReady(false);
        saveToConfig();
        scheduleRespawn(b, b.getRespawnSeconds());
    }

    private void consumeBoxCancel(Box b) {
        if (b.getEntityId() != null) {
            Entity e = findEntity(b.getEntityId());
            if (e != null) e.remove();
            entityToBox.remove(b.getEntityId());
            b.setEntityId(null);
        }
    }

    private void scheduleRespawn(final Box b, int seconds) {
        new BukkitRunnable() {
            @Override public void run() {
                b.setReady(true);
                spawnShulker(b);
                saveToConfig();
                String msg = plugin.msg("messages.box-restored").replace("{id}", String.valueOf(b.getId()));
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
            }
        }.runTaskLater(plugin, seconds * 20L);
    }

    private void spawnShulker(Box b) {
        // ensure no duplicate
        if (b.getEntityId() != null) {
            Entity prev = findEntity(b.getEntityId());
            if (prev != null) prev.remove();
            entityToBox.remove(b.getEntityId());
        }
        Location l = b.getLocation().clone();
        Shulker s = b.getLocation().getWorld().spawn(l, Shulker.class, sh -> {
            sh.setAI(false);
            sh.setPersistent(true);
            sh.setRemoveWhenFarAway(false);
            sh.setInvulnerable(true);
            sh.setCustomName(ChatColor.GOLD + "Лутбокс #" + b.getId());
            sh.setCustomNameVisible(true);
        });
        b.setEntityId(s.getUniqueId());
        entityToBox.put(s.getUniqueId(), b.getId());
    }

    private Entity findEntity(UUID id) {
        for (World w : Bukkit.getWorlds()) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }
}
