package io.github.tootertutor.modularpacks_curioscompat;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.tootertutor.modularpacks_curioscompat.compat.CuriosCompatImpl;
import io.github.tootertutor.modularpacks_curioscompat.compat.ModularPacksCompatImpl;
import io.github.tootertutor.modularpacks_curioscompat.config.ConfigLoader;
import io.github.tootertutor.modularpacks_curioscompat.listeners.BackpackClickListener;
import io.github.tootertutor.modularpacks_curioscompat.listeners.CuriosBackpackOpenListener;
import io.github.tootertutor.modularpacks_curioscompat.listeners.CuriosModuleBridgeService;

public class CuriosCompatPlugin extends JavaPlugin {

    private static CuriosCompatPlugin instance;
    private ConfigLoader config;
    private CuriosModuleBridgeService curiosModuleBridgeService;

    @Override
    public void onEnable() {
        instance = this;
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

        getLogger().info("Starting Curios module bridge...");
        this.curiosModuleBridgeService = new CuriosModuleBridgeService(this);
        this.curiosModuleBridgeService.start();

        getLogger().info("ModularPacks CuriosCompat has been enabled!");
    }

    @Override
    public void onDisable() {
        if (curiosModuleBridgeService != null) {
            curiosModuleBridgeService.stop();
            curiosModuleBridgeService = null;
        }

        getLogger().info("ModularPacks CuriosCompat has been disabled.");
    }

    public ConfigLoader getConfigLoader() {
        return config;
    }

    public static CuriosCompatPlugin getInstance() {
        return instance;
    }
}
