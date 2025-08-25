package dev.soldinbox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

import static dev.soldinbox.SoldInBox.getInstance;

public class BoxData {
    private final int id;
    private final Location location;

    public BoxData(int id, Location location) {
        this.id = id;
        this.location = location;
    }

    public void spawn() {
        if (location.getWorld() != null) {
            Block block = location.getBlock();
            block.setType(Material.SHULKER_BOX);
            BlockState state = block.getState();
            if (state instanceof Container container) {
                Inventory inv = container.getInventory();
                inv.clear();
                // Можно добавить дефолтный лут из конфига
            }
        }
    }

    public void openEditor(Player player) {
        if (location.getWorld() != null) {
            Block block = location.getBlock();
            BlockState state = block.getState();
            if (state instanceof Container container) {
                Inventory inv = container.getInventory();
                player.openInventory(inv);
            }
        }
    }
}
