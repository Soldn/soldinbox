package dev.soldinbox;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Listener for the editor GUI.
 * - Save button at slot 53 (index 53)
 * - Cancel button at slot 52 (index 52)
 * - Chance editing:
 *     Shift + LClick -> +10
 *     LClick         -> +1
 *     Shift + RClick -> -10
 *     RClick         -> -1
 */
public class GuiListener implements Listener {
    private final SoldInBox plugin;
    private final BoxManager manager;
    private final Map<UUID, Long> clickCooldown = new HashMap<>();

    public GuiListener(SoldInBox plugin, BoxManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private boolean tooFast(UUID u) {
        long now = System.currentTimeMillis();
        Long last = clickCooldown.get(u);
        long ms = plugin.getConfig().getLong("settings.click_cooldown_ms", 200L);
        if (last != null && now - last < ms) return true;
        clickCooldown.put(u, now);
        return false;
    }

    private boolean isEditor(InventoryView view) {
        if (view == null) return false;
        String title = view.getTitle();
        if (title == null) return false;
        String cfgTitle = plugin.getConfig().getString("messages.editor_title", "Редактор");
        // compare by prefix before ":" to allow dynamic "{id}" suffix in config
        String prefix = ChatColor.translateAlternateColorCodes('&', cfgTitle).split(":")[0];
        return title.startsWith(prefix);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        InventoryView view = e.getView();
        if (!isEditor(view)) return;

        // prevent default behavior in editor
        e.setCancelled(true);

        if (tooFast(p.getUniqueId())) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.click_too_fast")));
            return;
        }

        int slot = e.getRawSlot();
        ItemStack current = e.getCurrentItem();

        // Top inventory refers to the editor inventory
        Inventory top = view.getTopInventory();
        if (top == null) return;

        // Save button (index 53), Cancel (index 52)
        if (slot == 53) {
            if (top.getHolder() instanceof BoxData.EditorHolder holder) {
                int boxId = holder.boxId;
                List<LootEntry> newLoot = new ArrayList<>();
                for (ItemStack it : top.getContents()) {
                    if (it == null) continue;
                    // skip UI buttons
                    if (it.getType() == Material.EMERALD || it.getType() == Material.RED_WOOL) continue;
                    int amount = it.getAmount();
                    double chance = 10.0;
                    if (it.hasItemMeta() && it.getItemMeta().hasLore()) {
                        for (String line : it.getItemMeta().getLore()) {
                            String s = ChatColor.stripColor(line).toLowerCase(Locale.ROOT);
                            if (s.startsWith("шанс:") || s.startsWith("chance:")) {
                                String num = s.replaceAll("[^0-9.,]", "").replace(',', '.');
                                try {
                                    chance = Double.parseDouble(num);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    newLoot.add(new LootEntry(it.getType().name(), amount, chance));
                }
                BoxData box = manager.getBox(boxId);
                if (box != null) {
                    box.setLoot(newLoot);
                    manager.saveToFile();
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.reload")));
                }
            }
            p.closeInventory();
            return;
        } else if (slot == 52) { // Cancel
            p.closeInventory();
            return;
        }

        // Editing chances only in top inventory area (avoid player inventory)
        if (slot >= top.getSize()) return;
        if (current == null) return;
        if (!(top.getHolder() instanceof BoxData.EditorHolder)) return;

        // Determine delta by click type
        int delta = 0;
        if (e.isLeftClick() && e.isShiftClick()) delta = 10;
        else if (e.isLeftClick()) delta = 1;
        else if (e.isRightClick() && e.isShiftClick()) delta = -10;
        else if (e.isRightClick()) delta = -1;

        if (delta == 0) return;

        ItemMeta meta = current.getItemMeta();
        List<String> lore = (meta != null && meta.hasLore()) ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        double chance = 10.0;
        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            String line = ChatColor.stripColor(lore.get(i)).toLowerCase(Locale.ROOT);
            if (line.startsWith("шанс:") || line.startsWith("chance:")) {
                String num = line.replaceAll("[^0-9.,]", "").replace(',', '.');
                try { chance = Double.parseDouble(num); } catch (Exception ignored) {}
                chance = Math.max(0.0, Math.min(100.0, chance + delta));
                lore.set(i, "§7Шанс: §b" + String.format(Locale.ROOT, "%.1f", chance) + "%");
                found = true;
                break;
            }
        }
        if (!found) {
            chance = Math.max(0.0, Math.min(100.0, 10.0 + delta));
            lore.add("§7Шанс: §b" + String.format(Locale.ROOT, "%.1f", chance) + "%");
        }
        if (meta == null) meta = current.getItemMeta();
        meta.setLore(lore);
        current.setItemMeta(meta);

        // write back to top inventory slot (raw slot corresponds to top inventory)
        top.setItem(slot, current);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        InventoryView view = e.getView();
        if (!isEditor(view)) return;
        // we only save on explicit Save button, so do nothing here
    }
}
