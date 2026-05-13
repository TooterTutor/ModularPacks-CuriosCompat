package io.github.tootertutor.modularpacks_curioscompat.compat;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.inventory.ItemStack;

import io.github.tootertutor.ModularPacks.config.BackpackTypeDef;
import io.github.tootertutor.ModularPacks.item.BackpackItems;

public class CuriosCompatImpl {
    private static CuriosPaperAPI curiosAPI;
    private static BackpackItems backpackItems;
    private static boolean slotRegistered = false;

    public static void initialize() {
        try {
            CuriosPaper curiosPaper = CuriosPaper.getInstance();
            if (curiosPaper != null) {
                curiosAPI = curiosPaper.getCuriosPaperAPI();
            }
        } catch (Exception e) {
            // CuriosPaper may not be available
        }
    }

    public static void registerBackpackType(BackpackTypeDef backpackType) {
        if (curiosAPI == null || backpackType == null) {
            return;
        }

        try {
            // Register slot on first backpack type
            if (!slotRegistered) {
                registerSlotFromBackpackType(backpackType);
                slotRegistered = true;
            }

            // Create a sample ItemStack from the backpack type
            if (backpackItems == null) {
                backpackItems = new BackpackItems(
                        io.github.tootertutor.ModularPacks.api.ModularPacksAPI.getInstance().getPlugin());
            }

            ItemStack sampleItem = backpackItems.create(backpackType.id());
            if (sampleItem != null) {
                // Tag the item as a backpack accessory
                curiosAPI.tagAccessoryItem(sampleItem, "backpack");
            }
        } catch (Exception e) {
            // Error registering this backpack type
        }
    }

    private static void registerSlotFromBackpackType(BackpackTypeDef backpackType) {
        try {
            // Use the ModularPacks item model path for the resource pack
            String itemModel = "modularpacks:backpack/modularpacks-backpack";
            
            // Use custom model data if available for visual distinction
            Integer customModelData = backpackType.customModelData() > 0 ? backpackType.customModelData() : null;
            
            curiosAPI.registerSlot(
                    "backpack",
                    backpackType.displayName(),
                    backpackType.outputMaterial(),
                    itemModel,
                    customModelData,
                    1,
                    backpackType.lore());
        } catch (Exception e) {
            // Slot may already be registered or error occurred
        }
    }

    public static ItemStack tagAsBackpack(ItemStack item) {
        if (curiosAPI == null || item == null) {
            return item;
        }
        try {
            return curiosAPI.tagAccessoryItem(item, "backpack");
        } catch (Exception e) {
            return item;
        }
    }

    public static boolean isBackpackSlotRegistered() {
        return curiosAPI != null;
    }
}
