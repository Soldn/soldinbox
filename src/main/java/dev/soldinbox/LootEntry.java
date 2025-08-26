package dev.soldinbox;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LootEntry {
    private String materialName;
    private int amount;
    private int chance;

    public LootEntry(String materialName, int amount, int chance) {
        this.materialName = materialName;
        this.amount = amount;
        this.chance = Math.max(0, Math.min(100, chance));
    }

    public String getMaterialName() { return materialName; }
    public int getAmount() { return amount; }
    public int getChance() { return chance; }
    public void setChance(int c) { this.chance = Math.max(0, Math.min(100, c)); }

    public org.bukkit.inventory.ItemStack toItemStack() {
        Material m;
        try { m = Material.valueOf(materialName.toUpperCase()); } catch (Exception ex) { m = Material.STONE; }
        ItemStack is = new ItemStack(m, amount);
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(null);
            is.setItemMeta(meta);
        }
        return is;
    }
}
