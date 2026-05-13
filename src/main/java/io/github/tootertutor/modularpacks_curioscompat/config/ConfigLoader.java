package io.github.tootertutor.modularpacks_curioscompat.config;

import org.bukkit.configuration.file.FileConfiguration;

import io.github.tootertutor.modularpacks_curioscompat.CuriosCompatPlugin;

public class ConfigLoader {
    private final CuriosCompatPlugin plugin;
    private boolean modularPacksCuriosCompatEnabled = true;

    public ConfigLoader(CuriosCompatPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        this.modularPacksCuriosCompatEnabled = cfg.getBoolean("modularpacks.CuriosCompat.Enabled", true);
    }

    public boolean isModularPacksCuriosCompatEnabled() {
        return modularPacksCuriosCompatEnabled;
    }
}
