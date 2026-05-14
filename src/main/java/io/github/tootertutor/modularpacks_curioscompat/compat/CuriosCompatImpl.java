package io.github.tootertutor.modularpacks_curioscompat.compat;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import io.github.tootertutor.ModularPacks.config.BackpackTypeDef;

public class CuriosCompatImpl {
    private static CuriosPaperAPI curiosAPI;
    private static boolean slotRegistered = false;

    // CuriosPaper NBT tag keys
    private static final NamespacedKey CURIOS_ID_TAG = new NamespacedKey("curiospaper", "curios_custom_id");
    private static final NamespacedKey CURIOS_SLOT_TAG = new NamespacedKey("curiospaper", "curious_slot_type");

    public static void initialize() {
        try {
            CuriosPaper curiosPaper = CuriosPaper.getInstance();
            if (curiosPaper != null) {
                curiosAPI = curiosPaper.getCuriosPaperAPI();
                Bukkit.getLogger().info("[ModularPacks-CuriosCompat] CuriosPaper API initialized");
            } else {
                Bukkit.getLogger().warning("[ModularPacks-CuriosCompat] CuriosPaper instance is null");
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[ModularPacks-CuriosCompat] Error initializing CuriosPaper: " + e.getMessage());
        }
    }

    public static void registerBackpackType(BackpackTypeDef backpackType) {
        if (curiosAPI == null || backpackType == null) {
            Bukkit.getLogger().warning("[ModularPacks-CuriosCompat] Cannot register backpack type - curiosAPI is null or backpackType is null");
            return;
        }

        try {
            // Register slot on first backpack type
            if (!slotRegistered) {
                registerSlotFromBackpackType(backpackType);
                slotRegistered = true;
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[ModularPacks-CuriosCompat] Error registering backpack type: " + e.getMessage());
        }
    }

    private static void registerSlotFromBackpackType(BackpackTypeDef backpackType) {
        try {
            Integer customModelData = backpackType.customModelData() > 0 ? backpackType.customModelData() : null;

            Bukkit.getLogger().info("[ModularPacks-CuriosCompat] Registering backpack slot with material: " + backpackType.outputMaterial());

            curiosAPI.registerSlot(
                    "back",
                    "Backpack",
                    backpackType.outputMaterial(),
                    null,
                    customModelData,
                    1,
                    backpackType.lore());

            Bukkit.getLogger().info("[ModularPacks-CuriosCompat] Successfully registered backpack slot");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[ModularPacks-CuriosCompat] Error registering slot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static ItemStack tagBackpackDynamic(ItemStack item, String backpackType) {
        if (item == null || item.getItemMeta() == null) {
            return item;
        }

        try {
            ItemMeta meta = item.getItemMeta();

            // Tag with fixed curios_custom_id and the back slot type
            meta.getPersistentDataContainer().set(CURIOS_ID_TAG, PersistentDataType.STRING, "backpack");
            meta.getPersistentDataContainer().set(CURIOS_SLOT_TAG, PersistentDataType.STRING, "back");

            item.setItemMeta(meta);
            Bukkit.getLogger().info("[ModularPacks-CuriosCompat] Successfully tagged item with curios NBT");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ModularPacks-CuriosCompat] Error tagging backpack: " + e.getMessage());
        }

        return item;
    }

    public static void removeCuriosTags(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return;
        }

        try {
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().remove(CURIOS_ID_TAG);
            meta.getPersistentDataContainer().remove(CURIOS_SLOT_TAG);
            item.setItemMeta(meta);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ModularPacks-CuriosCompat] Error removing curios tags: " + e.getMessage());
        }
    }

    public static boolean isBackpackSlotRegistered() {
        return curiosAPI != null && slotRegistered;
    }
}
