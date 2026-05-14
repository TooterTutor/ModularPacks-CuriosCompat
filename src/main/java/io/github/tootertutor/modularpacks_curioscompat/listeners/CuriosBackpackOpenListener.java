package io.github.tootertutor.modularpacks_curioscompat.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import io.github.tootertutor.ModularPacks.api.ModularPacksAPI;
import io.github.tootertutor.ModularPacks.item.Keys;
import io.github.tootertutor.modularpacks_curioscompat.compat.CuriosCompatImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Listener for managing backpack interactions through the Curios slot.
 * 
 * Handles:
 * - Shift+right-click interactions to open equipped backpacks
 * - Block placement/pickup interference resolution
 * - Multi-backpack selection menu with pagination
 * 
 * @author ModularPacks-CuriosCompat
 */
public class CuriosBackpackOpenListener implements Listener {

    // Constants
    private static final String CURIOS_BACK_SLOT = "back";
    private static final int BACKPACKS_PER_PAGE = 3;
    private static final int PREV_ARROW_SLOT = 0;
    private static final int FIRST_BACKPACK_SLOT = 1;
    private static final int NEXT_ARROW_SLOT = 4;

    // State tracking
    private final Set<UUID> pendingTagRestoration = Collections.synchronizedSet(new HashSet<>());
    private final Map<UUID, Integer> backpackMenuPages = Collections.synchronizedMap(new HashMap<>());

    // ============================================================================
    // EVENT HANDLERS - Block Interaction Management
    // ============================================================================

    /**
     * Handles shift+right-click on blocks with backpacks.
     * Runs at LOWEST priority to intercept before other plugins (e.g.,
     * CuriosPaper).
     * 
     * Temporarily removes Curios equip tags to prevent CuriosPaper from
     * intercepting
     * the interaction, allowing ModularPacks to handle placement/pickup normally.
     * 
     * @param event the PlayerInteractEvent
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractLowest(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isBackpackItem(item)) {
            return;
        }

        // Temporarily remove Curios tags so CuriosPaper skips equipping
        CuriosCompatImpl.removeCuriosTags(item);
        player.getInventory().setItemInMainHand(item);
        pendingTagRestoration.add(player.getUniqueId());
    }

    /**
     * Restores Curios equip tags after block interaction.
     * Runs at MONITOR priority, after all other plugins have processed the event.
     * 
     * @param event the PlayerInteractEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteractMonitor(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!pendingTagRestoration.remove(player.getUniqueId())) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isBackpackItem(item)) {
            return;
        }

        String backpackType = getBackpackType(item);
        if (backpackType != null) {
            CuriosCompatImpl.tagBackpackDynamic(item, backpackType);
            player.getInventory().setItemInMainHand(item);
        }
    }

    // ============================================================================
    // EVENT HANDLERS - Backpack Opening
    // ============================================================================

    /**
     * Handles shift+right-click on air with empty main hand.
     * Opens equipped backpack(s) from the Curios slot.
     * 
     * @param event the PlayerInteractEvent
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Validate conditions for opening backpack menu
        if (!player.isSneaking()) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!player.getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }

        // Only open menu on air click; blocks are handled by other listeners
        if (action == Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Bukkit.getLogger().info("[ModularPacks-CuriosCompat] Shift+right-click detected, opening equipped backpack...");
        openEquippedBackpack(player);
        event.setCancelled(true);
    }

    // ============================================================================
    // EVENT HANDLERS - Menu Interaction
    // ============================================================================

    /**
     * Handles clicks within the backpack selection menu.
     * Routes clicks to either pagination handlers or backpack opening.
     * 
     * @param event the InventoryClickEvent
     */
    @EventHandler
    public void onBackpackMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackSelectionHolder)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            handlePaginationClick(event, player, clicked);
        } else {
            handleBackpackClick(event, player, clicked);
        }
    }

    // ============================================================================
    // PRIVATE METHODS - Backpack Opening Logic
    // ============================================================================

    /**
     * Attempts to open equipped backpack(s) from the Curios slot.
     * If only one backpack exists, opens it directly.
     * If multiple backpacks exist, displays a selection menu with pagination.
     * 
     * @param player the player opening the backpack
     */
    private void openEquippedBackpack(Player player) {
        try {
            CuriosPaperAPI curiosAPI = CuriosPaper.getInstance().getCuriosPaperAPI();
            ModularPacksAPI modularPacksAPI = ModularPacksAPI.getInstance();

            if (curiosAPI == null || modularPacksAPI == null) {
                Bukkit.getLogger().warning("[ModularPacks-CuriosCompat] APIs are null");
                return;
            }

            // Retrieve and filter equipped backpacks
            List<ItemStack> backpacksWithId = getValidEquippedBackpacks(player, curiosAPI);

            if (backpacksWithId.isEmpty()) {
                player.sendMessage(Component.text("No valid backpack equipped", NamedTextColor.GOLD));
                return;
            }

            // Open directly or show menu
            if (backpacksWithId.size() == 1) {
                openBackpack(player, backpacksWithId.get(0), modularPacksAPI);
            } else {
                backpackMenuPages.put(player.getUniqueId(), 0);
                showBackpackMenu(player, backpacksWithId, 0);
            }

        } catch (Exception e) {
            Bukkit.getLogger().severe("[ModularPacks-CuriosCompat] Error opening backpack: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(Component.text("Error opening backpack: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    /**
     * Retrieves valid equipped backpacks from the Curios slot.
     * Filters for items with valid backpack ID and type.
     * 
     * @param player    the player
     * @param curiosAPI the Curios API instance
     * @return list of valid backpacks, or empty list if none found
     */
    private List<ItemStack> getValidEquippedBackpacks(Player player, CuriosPaperAPI curiosAPI) {
        List<ItemStack> equippedItems = curiosAPI.getEquippedItems(player, CURIOS_BACK_SLOT);
        Bukkit.getLogger().info("[ModularPacks-CuriosCompat] Found "
                + (equippedItems != null ? equippedItems.size() : 0) + " equipped items");

        if (equippedItems == null || equippedItems.isEmpty()) {
            player.sendMessage(Component.text("No backpack equipped in Curios slot", NamedTextColor.GOLD));
            return Collections.emptyList();
        }

        List<ItemStack> backpacksWithId = new ArrayList<>();
        for (ItemStack item : equippedItems) {
            if (item != null && !item.getType().isAir()) {
                if (hasValidBackpackData(item)) {
                    backpacksWithId.add(item);
                }
            }
        }

        Bukkit.getLogger()
                .info("[ModularPacks-CuriosCompat] Found " + backpacksWithId.size() + " backpacks with valid IDs");
        return backpacksWithId;
    }

    /**
     * Opens a backpack for the player.
     * Uses ModularPacksAPI's BackpackMenuRenderer to display the backpack
     * inventory.
     * 
     * @param player   the player opening the backpack
     * @param backpack the backpack ItemStack
     * @param api      the ModularPacksAPI instance
     */
    private void openBackpack(Player player, ItemStack backpack, ModularPacksAPI api) {
        try {
            String backpackId = getBackpackId(backpack);
            String backpackType = getBackpackType(backpack);

            if (backpackId == null || backpackType == null) {
                return;
            }

            UUID backpackUUID = UUID.fromString(backpackId);
            Bukkit.getLogger().info(
                    "[ModularPacks-CuriosCompat] Opening backpack: id=" + backpackId + ", type=" + backpackType);

            api.getPlugin().getBackpackMenuRenderer().openMenu(player, backpackUUID, backpackType);

        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Error opening backpack: Invalid backpack ID", NamedTextColor.RED));
            Bukkit.getLogger().warning("[ModularPacks-CuriosCompat] Invalid backpack ID format: " + e.getMessage());
        } catch (Exception e) {
            player.sendMessage(Component.text("Error opening backpack: " + e.getMessage(), NamedTextColor.RED));
            Bukkit.getLogger().warning("[ModularPacks-CuriosCompat] Error opening backpack: " + e.getMessage());
        }
    }

    // ============================================================================
    // PRIVATE METHODS - Menu Management
    // ============================================================================

    /**
     * Displays the backpack selection menu with pagination.
     * Shows up to 3 backpacks per page with navigation arrows.
     * 
     * @param player    the player viewing the menu
     * @param backpacks the list of available backpacks
     * @param page      the current page (0-indexed)
     */
    private void showBackpackMenu(Player player, List<ItemStack> backpacks, int page) {
        Component menuTitle = Component.text("Select Backpack");
        BackpackSelectionHolder holder = new BackpackSelectionHolder(backpacks, page);
        Inventory menu = Bukkit.createInventory(holder, InventoryType.HOPPER, menuTitle);
        holder.setInventory(menu);

        // Populate backpack slots
        populateBackpackSlots(menu, backpacks, page);

        // Add pagination arrows
        addPaginationArrows(menu, backpacks.size(), page);

        player.openInventory(menu);
    }

    /**
     * Populates the menu with backpacks for the current page.
     * 
     * @param menu      the inventory menu
     * @param backpacks the list of backpacks
     * @param page      the current page
     */
    private void populateBackpackSlots(Inventory menu, List<ItemStack> backpacks, int page) {
        int startIndex = page * BACKPACKS_PER_PAGE;
        int endIndex = Math.min(startIndex + BACKPACKS_PER_PAGE, backpacks.size());

        int slotIndex = FIRST_BACKPACK_SLOT;
        for (int i = startIndex; i < endIndex; i++) {
            menu.setItem(slotIndex++, backpacks.get(i));
        }
    }

    /**
     * Adds pagination arrow buttons to the menu.
     * Shows previous arrow if not on first page, next arrow if not on last page.
     * 
     * @param menu           the inventory menu
     * @param totalBackpacks the total number of backpacks
     * @param currentPage    the current page (0-indexed)
     */
    private void addPaginationArrows(Inventory menu, int totalBackpacks, int currentPage) {
        int maxPage = (totalBackpacks + BACKPACKS_PER_PAGE - 1) / BACKPACKS_PER_PAGE - 1;

        if (currentPage > 0) {
            menu.setItem(PREV_ARROW_SLOT, createArrow("← Previous"));
        }

        if (currentPage < maxPage) {
            menu.setItem(NEXT_ARROW_SLOT, createArrow("Next →"));
        }
    }

    /**
     * Creates a display arrow item with the given label.
     * 
     * @param label the display name for the arrow
     * @return an ItemStack representing the arrow
     */
    private ItemStack createArrow(String label) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(label));
            arrow.setItemMeta(meta);
        }
        return arrow;
    }

    // ============================================================================
    // PRIVATE METHODS - Menu Click Handlers
    // ============================================================================

    /**
     * Handles arrow button clicks for pagination.
     * 
     * @param event  the inventory click event
     * @param player the player clicking
     * @param arrow  the arrow item clicked
     */
    private void handlePaginationClick(InventoryClickEvent event, Player player, ItemStack arrow) {
        BackpackSelectionHolder holder = (BackpackSelectionHolder) event.getInventory().getHolder();
        List<ItemStack> backpacks = holder.getBackpacks();
        int currentPage = holder.getCurrentPage();
        int maxPage = (backpacks.size() + BACKPACKS_PER_PAGE - 1) / BACKPACKS_PER_PAGE - 1;

        ItemMeta meta = arrow.getItemMeta();
        String displayName = (meta != null && meta.hasDisplayName()) ? meta.displayName().toString() : "";

        if (displayName.contains("←") && currentPage > 0) {
            showBackpackMenu(player, backpacks, currentPage - 1);
        } else if (displayName.contains("→") && currentPage < maxPage) {
            showBackpackMenu(player, backpacks, currentPage + 1);
        }
    }

    /**
     * Handles backpack item clicks to open the selected backpack.
     * 
     * @param event        the inventory click event
     * @param player       the player clicking
     * @param backpackItem the backpack item clicked
     */
    private void handleBackpackClick(InventoryClickEvent event, Player player, ItemStack backpackItem) {
        String backpackId = getBackpackId(backpackItem);
        String backpackType = getBackpackType(backpackItem);

        if (backpackId == null || backpackType == null) {
            return;
        }

        ModularPacksAPI api = ModularPacksAPI.getInstance();
        if (api != null) {
            openBackpack(player, backpackItem, api);
            player.closeInventory();
        }
    }

    // ============================================================================
    // PRIVATE METHODS - Data Extraction & Validation
    // ============================================================================

    /**
     * Checks if an item has valid backpack persistent data.
     * 
     * @param item the item to check
     * @return true if item has both backpack ID and type, false otherwise
     */
    private boolean hasValidBackpackData(ItemStack item) {
        return getBackpackId(item) != null && getBackpackType(item) != null;
    }

    /**
     * Checks if an item is a backpack.
     * 
     * @param item the item to check
     * @return true if item has a backpack ID, false otherwise
     */
    private boolean isBackpackItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }

        try {
            ModularPacksAPI api = ModularPacksAPI.getInstance();
            if (api == null) {
                return false;
            }

            Keys keys = api.getPlugin().keys();
            String backpackId = item.getItemMeta().getPersistentDataContainer()
                    .get(keys.BACKPACK_ID, PersistentDataType.STRING);

            return backpackId != null && !backpackId.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the backpack UUID from an item's persistent data.
     * 
     * @param item the item to extract from
     * @return the backpack ID as a string, or null if not found
     */
    private String getBackpackId(ItemStack item) {
        try {
            ModularPacksAPI api = ModularPacksAPI.getInstance();
            if (api == null) {
                return null;
            }

            Keys keys = api.getPlugin().keys();
            return item.getItemMeta().getPersistentDataContainer()
                    .get(keys.BACKPACK_ID, PersistentDataType.STRING);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the backpack type from an item's persistent data.
     * 
     * @param item the item to extract from
     * @return the backpack type, or null if not found
     */
    private String getBackpackType(ItemStack item) {
        try {
            ModularPacksAPI api = ModularPacksAPI.getInstance();
            if (api == null) {
                return null;
            }

            Keys keys = api.getPlugin().keys();
            return item.getItemMeta().getPersistentDataContainer()
                    .get(keys.BACKPACK_TYPE, PersistentDataType.STRING);
        } catch (Exception e) {
            return null;
        }
    }
}
