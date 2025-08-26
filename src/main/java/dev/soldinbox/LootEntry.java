package dev.soldinbox;

public class LootEntry {
    private String material;
    private int amount;
    private double chance;

    public LootEntry(String material, int amount, double chance) {
        this.material = material;
        this.amount = amount;
        this.chance = chance;
    }

    public String getMaterial() { return material; }
    public int getAmount() { return amount; }
    public double getChance() { return chance; }

    public void setChance(double chance) { this.chance = chance; }
}
