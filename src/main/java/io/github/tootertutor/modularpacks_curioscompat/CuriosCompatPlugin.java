package io.github.tootertutor.modularpacks_curioscompat;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.tootertutor.modularpacks_curioscompat.compat.CuriosCompatImpl;
import io.github.tootertutor.modularpacks_curioscompat.compat.ModularPacksCompatImpl;
import io.github.tootertutor.modularpacks_curioscompat.config.ConfigLoader;
import io.github.tootertutor.modularpacks_curioscompat.listeners.BackpackClickListener;
import io.github.tootertutor.modularpacks_curioscompat.listeners.CuriosBackpackOpenListener;

public class CuriosCompatPlugin extends JavaPlugin {

    private ConfigLoader config;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new ConfigLoader(this);
        this.config.reload();
        if (!config.isModularPacksCuriosCompatEnabled()) {
            getLogger().warning("modularpacks.CuriosCompat.Enabled is false. Plugin will remain idle.");
            return;
        }

        getLogger().info("Initializing CuriosPaper compatibility...");
        CuriosCompatImpl.initialize();

        getLogger().info("Registering ModularPacks backpack types...");
        ModularPacksCompatImpl.registerModularPacksCompat();

        getLogger().info("Registering event listeners...");
        getServer().getPluginManager().registerEvents(new BackpackClickListener(), this);
        getServer().getPluginManager().registerEvents(new CuriosBackpackOpenListener(), this);

        getLogger().info("ModularPacks CuriosCompat has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ModularPacks CuriosCompat has been disabled.");
    }
}
