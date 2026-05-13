package io.github.tootertutor.modularpacks_curioscompat.compat;

import java.util.Collection;

import io.github.tootertutor.ModularPacks.api.ModularPacksAPI;
import io.github.tootertutor.ModularPacks.config.BackpackTypeDef;

public class ModularPacksCompatImpl {

    public static void registerModularPacksCompat() {
        try {
            ModularPacksAPI api = ModularPacksAPI.getInstance();
            if (api == null) {
                return;
            }

            registerBackpackTypes(api);
        } catch (Exception e) {
            // ModularPacks API not available or error occurred
        }
    }

    private static void registerBackpackTypes(ModularPacksAPI api) {
        try {
            // Ensure CuriosPaper is initialized first
            if (!CuriosCompatImpl.isBackpackSlotRegistered()) {
                CuriosCompatImpl.initialize();
            }

            // Get all backpack types from ModularPacks
            Collection<BackpackTypeDef> backpackTypes = api.getPlugin().cfg().getTypes();

            if (backpackTypes.isEmpty()) {
                api.getPlugin().getLogger().info("No backpack types found in ModularPacks");
                return;
            }

            // Register each backpack type with CuriosPaper
            for (BackpackTypeDef backpackType : backpackTypes) {
                try {
                    CuriosCompatImpl.registerBackpackType(backpackType);
                    api.getPlugin().getLogger().info("Registered backpack type '" + backpackType.id() 
                            + "' with CuriosPaper (material: " + backpackType.outputMaterial() + ")");
                } catch (Exception e) {
                    api.getPlugin().getLogger()
                            .warning("Failed to register backpack type: " + backpackType.id() + " - " + e.getMessage());
                }
            }

            api.getPlugin().getLogger()
                    .info("Successfully registered " + backpackTypes.size() + " ModularPacks backpack types with CuriosPaper");
        } catch (Exception e) {
            // API method may not exist or have different name
        }
    }
}
