package dev.soldinbox;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    private final SoldInBox plugin;
    private final BoxManager manager;

    public CommandHandler(SoldInBox plugin, BoxManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (args.length == 2 && args[0].equalsIgnoreCase("edit")) {
            int id;
            try { id = Integer.parseInt(args[1]); } catch (NumberFormatException ex) { return true; }
            manager.openEditor(p, id);
            return true;
        }
        return false;
    }
}
