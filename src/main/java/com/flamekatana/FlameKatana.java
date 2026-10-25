package com.flamekatana;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlameKatana extends JavaPlugin {

    private KatanaItem katanaItem;
    private FlameSlash flameSlash;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        NamespacedKey key = new NamespacedKey(this, "flame_katana");
        this.katanaItem = new KatanaItem(key);
        this.flameSlash = new FlameSlash(this);

        getServer().getPluginManager().registerEvents(new KatanaListener(this), this);

        PluginCommand command = getCommand("katana");
        if (command != null) {
            KatanaCommand executor = new KatanaCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("FlameKatana enabled. Use /katana give to get the sword.");
    }

    public KatanaItem katana() {
        return katanaItem;
    }

    public FlameSlash slash() {
        return flameSlash;
    }
}
