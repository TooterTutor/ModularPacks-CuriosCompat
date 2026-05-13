package io.github.tootertutor.modularpacks_curioscompat;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.tootertutor.modularpacks_curioscompat.compat.CuriosCompatImpl;
import io.github.tootertutor.modularpacks_curioscompat.compat.ModularPacksCompatImpl;
import io.github.tootertutor.modularpacks_curioscompat.config.ConfigLoader;

public class CuriosCompatPlugin extends JavaPlugin {

    private ConfigLoader config;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new ConfigLoader(this);
        if (!config.isModularPacksCuriosCompatEnabled()) {
            getLogger().warning("modularpacks.CuriosCompat.Enabled is false. Plugin will remain idle.");
            return;
        }

        CuriosCompatImpl.initialize();
        ModularPacksCompatImpl.registerModularPacksCompat();

        getLogger().info("ModularPacks CuriosCompat has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ModularPacks CuriosCompat has been disabled.");
    }
}
