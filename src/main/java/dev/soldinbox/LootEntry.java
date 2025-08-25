package dev.soldinbox;

import org.bukkit.inventory.ItemStack;

public class LootEntry {
    private ItemStack item;
    private int chance;

    public LootEntry(ItemStack item, int chance) {
        this.item = item;
        this.chance = Math.max(0, Math.min(100, chance));
    }

    public ItemStack getItem() { return item; }
    public void setItem(ItemStack item) { this.item = item; }
    public int getChance() { return chance; }
    public void setChance(int chance) { this.chance = Math.max(0, Math.min(100, chance)); }
}
