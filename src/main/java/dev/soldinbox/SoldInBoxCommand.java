package dev.soldinbox;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SoldInBoxCommand implements CommandExecutor {
    private final SoldInBox plugin;
    private final BoxManager manager;

    public SoldInBoxCommand(SoldInBox plugin, BoxManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eSoldInBox commands: reload, reset, resetbox <id>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                manager.loadBoxes();
                sender.sendMessage("§aConfig reloaded!");
                break;
            case "reset":
                manager.spawnAll();
                sender.sendMessage("§aAll boxes respawned!");
                break;
            case "resetbox":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /soldinbox resetbox <id>");
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    manager.spawnBox(id);
                    sender.sendMessage("§aBox #" + id + " respawned!");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid ID");
                }
                break;
        }
        return true;
    }
}
