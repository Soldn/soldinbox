package dev.soldinbox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SoldInBox extends JavaPlugin implements Listener {

    private BoxManager boxManager;
    private long clickCooldownMs;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocal();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("SoldInBox enabled with " + boxManager.getBoxes().size() + " boxes.");
    }

    private void reloadLocal() {
        reloadConfig();
        this.clickCooldownMs = getConfig().getLong("settings.click-cooldown-ms", 200L);
        if (this.boxManager != null) {
            this.boxManager.shutdown();
        }
        this.boxManager = new BoxManager(this);
        this.boxManager.loadFromConfig();
        this.boxManager.spawnAllIfReady();
    }

    public String msg(String path) {
        String pfx = ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.prefix", ""));
        String body = ChatColor.translateAlternateColorCodes('&', getConfig().getString(path, path));
        return pfx + body;
    }

    // ==== Commands ====
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("soldinbox")) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player only.");
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("soldinbox.admin")) {
            p.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage(ChatColor.YELLOW + "/soldinbox reload|reset|resetbox <id>|setbox|edit <id>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                reloadLocal();
                p.sendMessage(msg("messages.reloaded"));
                break;
            case "reset":
                boxManager.resetAll();
                p.sendMessage(msg("messages.box-reset-all"));
                break;
            case "resetbox":
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Укажи id"); break; }
                try {
                    int id = Integer.parseInt(args[1]);
                    if (boxManager.resetBox(id)) {
                        p.sendMessage(msg("messages.box-reset-one").replace("{id}", String.valueOf(id)));
                    } else p.sendMessage(msg("messages.not-found"));
                } catch (NumberFormatException e) {
                    p.sendMessage(ChatColor.RED + "Неверный id");
                }
                break;
            case "setbox":
                // create new ID
                int id = boxManager.createBoxAt(p.getLocation());
                p.sendMessage(msg("messages.box-created").replace("{id}", String.valueOf(id)));
                break;
            case "edit":
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Укажи id"); break; }
                try {
                    int eid = Integer.parseInt(args[1]);
                    Box bx = boxManager.getBox(eid);
                    if (bx == null) { p.sendMessage(msg("messages.not-found")); break; }
                    openEditor(p, bx);
                } catch (NumberFormatException e) {
                    p.sendMessage(ChatColor.RED + "Неверный id");
                }
                break;
            default:
                p.sendMessage(ChatColor.YELLOW + "/soldinbox reload|reset|resetbox <id>|setbox|edit <id>");
        }
        return true;
    }

    private void openEditor(Player p, Box bx) {
        Inventory inv = Bukkit.createInventory(new EditorHolder(bx.getId()), 27, ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.editor-title", "Editor #{id}").replace("{id}", String.valueOf(bx.getId()))));
        // Fill with current loot as items with chance in lore
        int i = 0;
        for (LootEntry le : bx.getLoot()) {
            ItemStack clone = le.getItem().clone();
            ItemMeta meta = clone.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(ChatColor.GRAY + "Chance: " + ChatColor.AQUA + le.getChance() + "%");
            lore.add(ChatColor.DARK_GRAY + "ЛКМ +5%  |  ПКМ -5%  |  Shift x2");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            clone.setItemMeta(meta);
            inv.setItem(i++, clone);
            if (i >= inv.getSize()) break;
        }
        p.openInventory(inv);
        editorViewers.add(p.getUniqueId());
        editingBox.put(p.getUniqueId(), bx.getId());
    }

    // Track editor viewers and cooldowns
    private final Set<UUID> editorViewers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> editingBox = new ConcurrentHashMap<>();
    private final Map<UUID, Long> clickCooldown = new ConcurrentHashMap<>();

    private boolean tooFast(UUID u) {
        long now = System.currentTimeMillis();
        Long last = clickCooldown.get(u);
        if (last != null && now - last < clickCooldownMs) return true;
        clickCooldown.put(u, now);
        return false;
    }

    // ==== Events ====
    @EventHandler
    public void onInteractShulker(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof Shulker)) return;
        Shulker s = (Shulker) e.getRightClicked();
        Box bx = boxManager.getByEntity(s.getUniqueId());
        if (bx == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!bx.isReady()) {
            p.sendMessage(msg("messages.not-ready"));
            return;
        }
        // Open loot inventory
        Inventory inv = Bukkit.createInventory(new LootHolder(bx.getId()), 27, ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages.open-title", "Lootbox #{id}").replace("{id}", String.valueOf(bx.getId()))));
        boxManager.fillInventoryWithLoot(bx, inv);
        p.openInventory(inv);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntityType() != EntityType.SHULKER) return;
        Box bx = boxManager.getByEntity(e.getEntity().getUniqueId());
        if (bx != null) {
            e.setCancelled(true); // защищаем "бокс"
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        if (inv.getHolder() instanceof LootHolder) {
            if (tooFast(p.getUniqueId())) {
                e.setCancelled(true);
                return;
            }
            // allow normal take, but anti-shift-click spam
            if (e.isShiftClick()) {
                e.setCancelled(true);
                return;
            }
        } else if (inv.getHolder() instanceof EditorHolder) {
            e.setCancelled(true);
            if (!editorViewers.contains(p.getUniqueId())) return;
            Integer bxId = editingBox.get(p.getUniqueId());
            if (bxId == null) return;
            Box bx = boxManager.getBox(bxId);
            if (bx == null) return;

            ItemStack current = e.getCurrentItem();
            if (current == null) return;

            // Adjust chance via clicks
            boolean shift = e.isShiftClick() || p.isSneaking();
            int delta = (e.isLeftClick() ? +5 : e.isRightClick() ? -5 : 0);
            if (delta != 0) {
                if (shift) delta *= 2;
                // find corresponding loot entry by similarity
                LootEntry target = bx.findMatchingLoot(current);
                if (target != null) {
                    int newChance = Math.max(0, Math.min(100, target.getChance() + delta));
                    target.setChance(newChance);
                    // refresh item lore
                    ItemStack updated = current.clone();
                    ItemMeta meta = updated.getItemMeta();
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    // rewrite chance line
                    List<String> newLore = new ArrayList<>();
                    boolean replaced = false;
                    for (String line : lore) {
                        if (ChatColor.stripColor(line).toLowerCase().startsWith("chance:")) {
                            newLore.add(ChatColor.GRAY + "Chance: " + ChatColor.AQUA + newChance + "%");
                            replaced = true;
                        } else newLore.add(line);
                    }
                    if (!replaced) newLore.add(ChatColor.GRAY + "Chance: " + ChatColor.AQUA + newChance + "%");
                    meta.setLore(newLore);
                    updated.setItemMeta(meta);
                    inv.setItem(e.getSlot(), updated);
                    p.updateInventory();
                }
            }
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof EditorHolder) {
            Player p = (Player) e.getPlayer();
            editorViewers.remove(p.getUniqueId());
            Integer bxId = editingBox.remove(p.getUniqueId());
            if (bxId != null) {
                // Save current inventory as loot set (items non-air get equal default chance if missing)
                Box bx = boxManager.getBox(bxId);
                if (bx != null) {
                    List<LootEntry> list = new ArrayList<>();
                    for (ItemStack it : e.getInventory().getContents()) {
                        if (it == null || it.getType().isAir()) continue;
                        int chance = 10;
                        ItemMeta meta = it.getItemMeta();
                        if (meta != null && meta.hasLore()) {
                            for (String line : meta.getLore()) {
                                String s = ChatColor.stripColor(line).toLowerCase();
                                if (s.startsWith("chance:")) {
                                    String num = s.replace("chance:", "").replace("%", "").trim();
                                    try { chance = Integer.parseInt(num); } catch (Exception ignore) {}
                                }
                            }
                        }
                        ItemStack clean = it.clone();
                        if (clean.hasItemMeta()) {
                            ItemMeta m = clean.getItemMeta();
                            if (m.hasLore()) { m.setLore(null); }
                            clean.setItemMeta(m);
                        }
                        list.add(new LootEntry(clean, chance));
                    }
                    bx.setLoot(list);
                    getBoxManager().saveToConfig(); // persist
                }
            }
        } else if (e.getInventory().getHolder() instanceof LootHolder) {
            // Loot was opened; mark box consumed and start respawn
            LootHolder h = (LootHolder) e.getInventory().getHolder();
            Box bx = boxManager.getBox(h.boxId);
            if (bx != null && bx.isReady()) {
                boxManager.consumeBox(bx);
            }
        }
    }

    public BoxManager getBoxManager() { return boxManager; }

    // Holders for inventories
    public static class LootHolder implements InventoryHolder {
        final int boxId;
        LootHolder(int id) { this.boxId = id; }
        @Override public Inventory getInventory() { return null; }
    }
    public static class EditorHolder implements InventoryHolder {
        final int boxId;
        EditorHolder(int id) { this.boxId = id; }
        @Override public Inventory getInventory() { return null; }
    }
}
