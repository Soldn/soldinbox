package dev.soldinbox;

import org.bukkit.plugin.java.JavaPlugin;

public class SoldInBox extends JavaPlugin {
    private static SoldInBox instance;
    private BoxManager boxManager;
    private GuiListener guiListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        // load boxes.yml if not exists
        getDataFolder().mkdirs();
        saveResource("boxes.yml", false);
        this.boxManager = new BoxManager(this);
        this.guiListener = new GuiListener(this, boxManager);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getCommand("soldinbox").setExecutor(new CommandHandler(this, boxManager));
        getLogger().info("SoldInBox v5 enabled");
        boxManager.spawnAll();
    }

    @Override
    public void onDisable() {
        getLogger().info("SoldInBox disabled");
    }

    public static SoldInBox getInstance() {
        return instance;
    }

    public BoxManager getBoxManager() {
        return boxManager;
    }
}
