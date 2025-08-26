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
        String title = view.getTitle();
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
            // Здесь надо сохранить новый лут в коробку
            p.closeInventory();
            return;
        } else if (slot == 52) { // Отмена
            p.closeInventory();
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {}
}
