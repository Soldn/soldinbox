package dev.soldinbox;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

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

    private boolean isEditor(Inventory inv) {
        if (inv == null) return false;
        String title = inv.getTitle();
        if (title == null) return false;
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.editor_title", "Editor")).split(":")[0];
        return title.startsWith(prefix);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        Inventory inv = e.getInventory();
        if (inv == null) return;
        if (!isEditor(inv)) return;
        e.setCancelled(true); // control movement ourselves
        if (tooFast(p.getUniqueId())) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.click_too_fast")));
            return;
        }
        int slot = e.getRawSlot();
        ItemStack current = e.getCurrentItem();
        if (slot==26) { // save
            if (inv.getHolder() instanceof dev.soldinbox.BoxData.EditorHolder holder) {
                int boxId = holder.boxId;
                List<LootEntry> newLoot = new ArrayList<>();
                for (ItemStack it : inv.getContents()) {
                    if (it==null) continue;
                    if (it.getType()==Material.EMERALD || it.getType()==Material.RED_WOOL) continue;
                    int amount = it.getAmount();
                    int chance = 10;
                    if (it.hasItemMeta() && it.getItemMeta().hasLore()) {
                        for (String line : it.getItemMeta().getLore()) {
                            String s = ChatColor.stripColor(line).toLowerCase();
                            if (s.startsWith("шанс:") || s.startsWith("chance:")) {
                                String num = s.replaceAll("[^0-9]", "");
                                try { chance = Integer.parseInt(num); } catch (Exception ignored) {}
                            }
                        }
                    }
                    newLoot.add(new LootEntry(it.getType().name(), amount, chance));
                }
                dev.soldinbox.BoxData box = manager.getBox(boxId);
                if (box!=null) {
                    box.setLoot(newLoot);
                    manager.saveToConfig();
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.reload")));
                }
                p.closeInventory();
                return;
            }
        } else if (slot==25) { // cancel
            p.closeInventory();
            return;
        }

        // edit chance on click
        if (current==null) return;
        if (!(inv.getHolder() instanceof dev.soldinbox.BoxData.EditorHolder holder)) return;
        int boxId = holder.boxId;
        int delta = 0;
        if (e.isLeftClick()) delta = 5;
        if (e.isRightClick()) delta = -5;
        if (e.isShiftClick()) delta *= 2;
        if (delta!=0) {
            ItemMeta meta = current.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            int chance = 10;
            boolean found=false;
            for (int i=0;i<lore.size();i++) {
                String line = ChatColor.stripColor(lore.get(i)).toLowerCase();
                if (line.startsWith("шанс:") || line.startsWith("chance:")) {
                    String num = line.replaceAll("[^0-9]", "");
                    try { chance = Integer.parseInt(num); } catch (Exception ignored) {}
                    chance = Math.max(0, Math.min(100, chance + delta));
                    lore.set(i, "§7Шанс: §b" + chance + "%");
                    found=true;
                    break;
                }
            }
            if (!found) {
                chance = Math.max(0, Math.min(100, 10 + delta));
                lore.add("§7Шанс: §b" + chance + "%");
            }
            meta.setLore(lore);
            current.setItemMeta(meta);
            inv.setItem(e.getSlot(), current);
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        // do nothing - saving only on explicit Save button
    }
}
