package dev.soldinbox;

public class LootEntry {
    private final String material;
    private final int amount;
    private final double chance;

    public LootEntry(String material, int amount, double chance) {
        this.material = material;
        this.amount = amount;
        this.chance = chance;
    }

    public String getMaterial() { return material; }
    public int getAmount() { return amount; }
    public double getChance() { return chance; }
}
