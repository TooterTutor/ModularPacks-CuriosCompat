package io.github.tootertutor.modularpacks_curioscompat.listeners;

import java.util.List;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Inventory holder for the backpack selection menu.
 * 
 * Stores metadata about the displayed backpack selection inventory, including
 * the list of available backpacks and the current pagination state.
 * 
 * @author ModularPacks-CuriosCompat
 */
public class BackpackSelectionHolder implements InventoryHolder {

    private Inventory inventory;
    private final List<ItemStack> backpacks;
    private final int currentPage;

    /**
     * Creates a new backpack selection menu holder.
     * 
     * @param backpacks   the list of backpack items to display
     * @param currentPage the current page (0-indexed)
     */
    public BackpackSelectionHolder(List<ItemStack> backpacks, int currentPage) {
        this.inventory = null;
        this.backpacks = backpacks;
        this.currentPage = currentPage;
    }

    /**
     * Sets the inventory for this holder.
     * 
     * @param inventory the inventory to set
     */
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Gets the inventory for this holder.
     * 
     * @return the inventory, or null if not yet set
     */
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Gets the list of available backpacks.
     * 
     * @return the backpack items
     */
    public List<ItemStack> getBackpacks() {
        return backpacks;
    }

    /**
     * Gets the current page of the display.
     * 
     * @return the current page (0-indexed)
     */
    public int getCurrentPage() {
        return currentPage;
    }
}
