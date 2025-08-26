package dev.soldinbox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class BoxData {
    private final int id;
    private final Location location;
    private int respawnSeconds;
    private List<LootEntry> loot;
    private boolean ready = true;

    public BoxData(int id, Location location, int respawnSeconds, List<LootEntry> loot) {
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

    public void spawn() {
        if (location.getWorld() == null) return;
        Block block = location.getBlock();
        block.setType(Material.SHULKER_BOX);
        BlockState state = block.getState();
        if (state instanceof Container container) {
            Inventory inv = container.getInventory();
            inv.clear();
            // populate from loot list
            for (LootEntry le : loot) {
                ItemStack is = le.toItemStack();
                ItemMeta meta = is.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(ChatColor.GRAY + "Шанс: " + ChatColor.AQUA + le.getChance() + "%");
                meta.setLore(lore);
                is.setItemMeta(meta);
                inv.addItem(is);
            }
            state.update(true, true); // important: update so players see items
        }
    }

    public void clearBlock() {
        if (location.getWorld() == null) return;
        Block block = location.getBlock();
        block.setType(Material.AIR);
    }

    public void openEditor(Player player) {
        Inventory inv = org.bukkit.Bukkit.createInventory(new EditorHolder(id), 54,
                ChatColor.translateAlternateColorCodes('&', SoldInBox.getInstance().getConfig().getString("messages.editor_title").replace("{id}", String.valueOf(id))));
        // fill with current loot (attach chance in lore)
        int i=0;
        for (LootEntry le : loot) {
            ItemStack is = le.toItemStack();
            ItemMeta meta = is.getItemMeta();
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(ChatColor.GRAY + "Шанс: " + ChatColor.AQUA + le.getChance() + "%");
            meta.setLore(lore);
            is.setItemMeta(meta);
            inv.setItem(i++, is);
            if (i>=inv.getSize()) break;
        }
        // add save and cancel buttons
        ItemStack save = new ItemStack(Material.EMERALD);
        ItemMeta sm = save.getItemMeta(); sm.setDisplayName(ChatColor.GREEN + "Сохранить"); save.setItemMeta(sm);
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cm = cancel.getItemMeta(); cm.setDisplayName(ChatColor.RED + "Отмена"); cancel.setItemMeta(cm);
        inv.setItem(53, save);
        inv.setItem(52, cancel);
        player.openInventory(inv);
    }

    public static class EditorHolder implements org.bukkit.inventory.InventoryHolder {
        public final int boxId;
        public EditorHolder(int id) { this.boxId = id; }
        @Override public Inventory getInventory() { return null; }
    }
}
