package dev.soldinbox;

import java.util.List;

public class BoxData {
    private int id;
    private List<LootEntry> loot;

    public BoxData(int id, List<LootEntry> loot) {
        this.id = id;
        this.loot = loot;
    }

    public List<LootEntry> getLoot() {
        return loot;
    }

    public void setLoot(List<LootEntry> loot) {
        this.loot = loot;
    }

    public int getId() {
        return id;
    }

    public static class EditorHolder {} // Заглушка для GUI
}
