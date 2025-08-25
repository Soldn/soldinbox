package dev.soldinbox;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SoldInBoxCommand implements CommandExecutor {
    private final SoldInBox plugin;
    private final BoxManager manager;

    public SoldInBoxCommand(SoldInBox plugin, BoxManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) {
            player.sendMessage("§eКоманды: reload, reset, resetbox <id>, setbox");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                manager.loadBoxes();
                player.sendMessage(plugin.getConfig().getString("messages.reload"));
            }
            case "reset" -> {
                manager.spawnAll();
                player.sendMessage(plugin.getConfig().getString("messages.box_reset_all"));
            }
            case "resetbox" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfig().getString("messages.not_found"));
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    manager.spawnBox(id);
                    player.sendMessage(plugin.getConfig().getString("messages.box_reset_one").replace("{id}", args[1]));
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfig().getString("messages.not_found"));
                }
            }
            case "setbox" -> {
                player.sendMessage(plugin.getConfig().getString("messages.box_created").replace("{id}", "new"));
                // TODO: Добавить генерацию нового id и запись в конфиг
            }
            default -> player.sendMessage("§cНеизвестная команда");
        }
        return true;
    }
}
