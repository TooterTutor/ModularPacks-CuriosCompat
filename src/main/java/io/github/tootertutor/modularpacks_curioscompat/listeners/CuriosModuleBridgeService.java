package io.github.tootertutor.modularpacks_curioscompat.listeners;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import io.github.tootertutor.ModularPacks.api.ModularPacksAPI;
import io.github.tootertutor.ModularPacks.config.BackpackTypeDef;
import io.github.tootertutor.ModularPacks.data.BackpackData;
import io.github.tootertutor.ModularPacks.gui.BackpackMenuHolder;
import io.github.tootertutor.ModularPacks.item.BackpackItems;
import io.github.tootertutor.ModularPacks.item.Keys;
import io.github.tootertutor.modularpacks_curioscompat.CuriosCompatPlugin;

/**
 * Bridges Curios-equipped backpacks into ModularPacks' carried-backpack engine
 * so passive modules still tick while the backpack is equipped in Curios.
 */
public final class CuriosModuleBridgeService {

    private static final String CURIOS_BACK_SLOT = "back";
    private static final long TICK_PERIOD = 10L;

    private final CuriosCompatPlugin plugin;
    private BukkitTask task;

    private Object modularPacksEngines;
    private Method tickBackpackMethod;
    private BackpackItems backpackItems;
    private boolean hookReady;

    public CuriosModuleBridgeService(CuriosCompatPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            return;
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickBridge, TICK_PERIOD, TICK_PERIOD);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tickBridge() {
        try {
            ModularPacksAPI modularPacksAPI = ModularPacksAPI.getInstance();
            CuriosPaper curiosPaper = CuriosPaper.getInstance();
            if (modularPacksAPI == null || curiosPaper == null) {
                return;
            }

            CuriosPaperAPI curiosAPI = curiosPaper.getCuriosPaperAPI();
            if (curiosAPI == null) {
                return;
            }

            if (!ensureHookReady(modularPacksAPI)) {
                return;
            }

            Keys keys = modularPacksAPI.getPlugin().keys();
            Set<UUID> openModuleIds = new HashSet<>();
            Set<UUID> openBackpackIds = collectOpenBackpackIds();

            for (Player player : Bukkit.getOnlinePlayers()) {
                processPlayer(player, curiosAPI, modularPacksAPI, keys, openModuleIds, openBackpackIds);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Curios module bridge tick failed: " + e.getMessage());
        }
    }

    private Set<UUID> collectOpenBackpackIds() {
        Set<UUID> openBackpackIds = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() == null || player.getOpenInventory().getTopInventory() == null) {
                continue;
            }

            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BackpackMenuHolder holder) {
                openBackpackIds.add(holder.backpackId());
            }
        }
        return openBackpackIds;
    }

    private void processPlayer(Player player, CuriosPaperAPI curiosAPI, ModularPacksAPI modularPacksAPI, Keys keys,
            Set<UUID> openModuleIds, Set<UUID> openBackpackIds) {

        List<ItemStack> equippedItems = curiosAPI.getEquippedItems(player, CURIOS_BACK_SLOT);
        if (equippedItems == null || equippedItems.isEmpty()) {
            return;
        }

        Set<UUID> inventoryBackpackIds = collectInventoryBackpackIds(player, keys);
        Set<UUID> processedBackpacks = new HashSet<>();

        for (int slotIndex = 0; slotIndex < equippedItems.size(); slotIndex++) {
            ItemStack item = equippedItems.get(slotIndex);
            UUID backpackId = readBackpackId(keys, item);
            if (backpackId == null) {
                continue;
            }

            String backpackType = readBackpackType(keys, item);
            if (backpackType == null || backpackType.isBlank()) {
                continue;
            }

            // Skip duplicate ticks for multiple references to the same backpack.
            if (processedBackpacks.add(backpackId) && !inventoryBackpackIds.contains(backpackId)) {
                invokeTickBackpack(player, backpackId, backpackType, openModuleIds, openBackpackIds);
            }

            refreshEquippedBackpackItem(curiosAPI, modularPacksAPI, player, slotIndex, item, backpackId, backpackType);
        }
    }

    private void refreshEquippedBackpackItem(CuriosPaperAPI curiosAPI, ModularPacksAPI modularPacksAPI,
            Player player, int slotIndex, ItemStack equippedItem, UUID backpackId, String backpackType) {

        BackpackTypeDef typeDef = modularPacksAPI.getPlugin().cfg().findType(backpackType);
        if (typeDef == null) {
            return;
        }

        BackpackData data = modularPacksAPI.getPlugin().repo().loadOrCreate(backpackId, backpackType);
        if (data == null) {
            return;
        }

        int totalSlots = typeDef.rows() * 9;
        if (!backpackItems.refreshInPlace(equippedItem, typeDef, backpackId, data, totalSlots)) {
            return;
        }

        curiosAPI.setEquippedItem(player, CURIOS_BACK_SLOT, slotIndex, equippedItem);
    }

    private Set<UUID> collectInventoryBackpackIds(Player player, Keys keys) {
        Set<UUID> ids = new HashSet<>();
        ItemStack[] contents = player.getInventory().getContents();
        if (contents == null || contents.length == 0) {
            return ids;
        }

        for (ItemStack item : contents) {
            UUID id = readBackpackId(keys, item);
            if (id != null) {
                ids.add(id);
            }
        }

        return ids;
    }

    private void invokeTickBackpack(Player player, UUID backpackId, String backpackType,
            Set<UUID> openModuleIds, Set<UUID> openBackpackIds) {
        try {
            tickBackpackMethod.invoke(modularPacksEngines, player, backpackId, backpackType, openModuleIds,
                    openBackpackIds);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to tick Curios backpack modules: " + e.getMessage());
        }
    }

    private boolean ensureHookReady(ModularPacksAPI api) {
        if (hookReady) {
            return true;
        }

        try {
            Field enginesField = api.getPlugin().getClass().getDeclaredField("engines");
            enginesField.setAccessible(true);
            modularPacksEngines = enginesField.get(api.getPlugin());
            if (modularPacksEngines == null) {
                return false;
            }

            tickBackpackMethod = modularPacksEngines.getClass().getDeclaredMethod(
                    "tickBackpack",
                    Player.class,
                    UUID.class,
                    String.class,
                    Set.class,
                    Set.class);
            tickBackpackMethod.setAccessible(true);

            backpackItems = new BackpackItems(api.getPlugin());

            hookReady = true;
            plugin.getLogger().info("Curios module bridge initialized.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to initialize Curios module bridge hook: " + e.getMessage());
            return false;
        }
    }

    private UUID readBackpackId(Keys keys, ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        String id = item.getItemMeta().getPersistentDataContainer().get(keys.BACKPACK_ID, PersistentDataType.STRING);
        if (id == null || id.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String readBackpackType(Keys keys, ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        return item.getItemMeta().getPersistentDataContainer().get(keys.BACKPACK_TYPE, PersistentDataType.STRING);
    }
}
