package dev.soldinbox;

import org.bukkit.plugin.java.JavaPlugin;

public class SoldInBox extends JavaPlugin {
    private static SoldInBox instance;
    private BoxManager boxManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.boxManager = new BoxManager(this);
        getCommand("soldinbox").setExecutor(new SoldInBoxCommand(this, boxManager));
        getLogger().info("SoldInBox enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SoldInBox disabled!");
    }

    public static SoldInBox getInstance() {
        return instance;
    }
}
