package dev.soldinbox;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BoxManager {
    private final SoldInBox plugin;
    private final Map<Integer, BoxData> boxes = new HashMap<>();

    public BoxManager(SoldInBox plugin) {
        this.plugin = plugin;
    }

    public void loadFromConfig() {
        // Загрузка коробок из конфигурации
    }

    public void saveToFile() {
        // Сохранение коробок в конфигурацию
    }

    public void resetAll() {
        boxes.clear();
    }

    public void resetBox(int id) {
        boxes.remove(id);
    }

    public void createBoxAt(Location loc) {
        // Создать новую коробку в локации
    }

    public void openEditor(Player p, int boxId) {
        // Открыть GUI редактора для игрока
    }

    public void spawnAll() {
        // Спавн всех коробок на сервере
    }

    public BoxData getBox(int id) {
        return boxes.get(id);
    }
}
