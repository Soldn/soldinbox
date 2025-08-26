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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only player"); return true; }
        Player p = (Player) sender;
        if (!p.hasPermission("soldinbox.admin")) {
            p.sendMessage("§cНет прав");
            return true;
        }
        if (args.length==0) { p.sendMessage("§e/soldinbox reload|reset|resetbox <id>|setbox|edit <id>"); return true; }
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                manager.loadFromConfig();
                p.sendMessage(plugin.getConfig().getString("messages.reload"));
                break;
            case "reset":
                manager.resetAll();
                p.sendMessage(plugin.getConfig().getString("messages.box_reset_all"));
                break;
            case "resetbox":
                if (args.length<2) { p.sendMessage("§cУкажи id"); break; }
                try {
                    int id = Integer.parseInt(args[1]);
                    manager.resetBox(id);
                    p.sendMessage(plugin.getConfig().getString("messages.box_reset_one").replace("{id}", String.valueOf(id)));
                } catch (NumberFormatException ex) { p.sendMessage("§cНеверный id"); }
                break;
            case "setbox":
                int id = manager.createBoxAt(p.getLocation());
                p.sendMessage(plugin.getConfig().getString("messages.box_created").replace("{id}", String.valueOf(id)));
                break;
            case "edit":
                if (args.length<2) { p.sendMessage("§cУкажи id"); break; }
                try {
                    int eid = Integer.parseInt(args[1]);
                    manager.openEditor(p, eid);
                } catch (NumberFormatException ex) { p.sendMessage("§cНеверный id"); }
                break;
            default:
                p.sendMessage("§e/soldinbox reload|reset|resetbox <id>|setbox|edit <id>");
        }
        return true;
    }
}
