package dev.soldinbox;

import org.bukkit.plugin.java.JavaPlugin;

public final class SoldInBox extends JavaPlugin {
    private BoxManager boxManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        boxManager = new BoxManager(this);

        // Регистрируем слушателей и команды
        getServer().getPluginManager().registerEvents(new GuiListener(this, boxManager), this);
        getCommand("soldinbox").setExecutor(new CommandHandler(this, boxManager));

        // Спавн всех коробок
        boxManager.spawnAll();
    }

    @Override
    public void onDisable() {
        // Сохраняем все данные
        boxManager.saveToFile();
    }

    public BoxManager getBoxManager() {
        return boxManager;
    }
}
