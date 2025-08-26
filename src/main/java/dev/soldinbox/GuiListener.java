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
        Inventory top = view.getTopInventory();
        if (top == null) return false;
        String title = top.getTitle(); // Исправлено для 1.20+
        if (title == null) return false;
        String cfgTitle = plugin.getConfig().getString("messages.editor_title", "Редактор");
        return title.startsWith(ChatColor.translateAlternateColorCodes('&', cfgTitle).split(":")[0]);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        InventoryView view = e.getView();
        if (!isEditor(view)) return;

        e.setCancelled(true);
        if (tooFast(p.getUniqueId())) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.click_too_fast")));
            return;
        }

        int slot = e.getRawSlot();
        Inventory top = view.getTopInventory();
        if (top == null) return;

        // Сохранение
        if (slot == 53) {
            if (top.getHolder() instanceof BoxData.EditorHolder) {
                int boxId = Integer.parseInt(top.getTitle().replaceAll("[^0-9]", ""));
                BoxData box = manager.getBox(boxId);
                if (box != null) {
                    List<LootEntry> newLoot = new ArrayList<>();
                    for (ItemStack it : top.getContents()) {
                        if (it == null || it.getType() == Material.AIR) continue;
                        double chance = 10.0;
                        if (it.hasItemMeta() && it.getItemMeta().hasLore()) {
                            for (String line : it.getItemMeta().getLore()) {
                                String s = ChatColor.stripColor(line).toLowerCase(Locale.ROOT);
                                if (s.startsWith("шанс:")) {
                                    String num = s.replaceAll("[^0-9.,]", "").replace(',', '.');
                                    try { chance = Double.parseDouble(num); } catch (Exception ignored) {}
                                }
                            }
                        }
                        newLoot.add(new LootEntry(it.getType().name(), it.getAmount(), chance));
                    }
                    box.setLoot(newLoot);
                    manager.saveToFile();
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.reload")));
                }
            }
            p.closeInventory();
            return;
        } else if (slot == 52) { // Отмена
            p.closeInventory();
            return;
        }

        // Изменение шанса
        if (slot >= top.getSize()) return;
        ItemStack current = top.getItem(slot);
        if (current == null || current.getType() == Material.AIR) return;

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
            if (line.startsWith("шанс:")) {
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
        top.setItem(slot, current);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        // Сохраняем только по кнопке Сохранить
    }
}
