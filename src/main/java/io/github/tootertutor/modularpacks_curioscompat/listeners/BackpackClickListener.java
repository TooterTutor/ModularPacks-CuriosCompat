package io.github.tootertutor.modularpacks_curioscompat.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import io.github.tootertutor.ModularPacks.api.ModularPacksAPI;
import io.github.tootertutor.ModularPacks.item.Keys;
import io.github.tootertutor.modularpacks_curioscompat.compat.CuriosCompatImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class BackpackClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();

        // Tag cursor item if it's a backpack
        if (cursor != null && cursor.hasItemMeta() && isBackpackItem(cursor)) {
            String backpackType = getBackpackType(cursor);
            if (backpackType != null) {
                CuriosCompatImpl.tagBackpackDynamic(cursor, backpackType);
                player.sendActionBar(Component.text("Backpack ready for Curios slot", NamedTextColor.GOLD));
                Bukkit.getLogger().info("[ModularPacks-CuriosCompat] Tagged cursor backpack: " + backpackType);
            }
        }

        // Tag current item if it's a backpack
        if (current != null && current.hasItemMeta() && isBackpackItem(current)) {
            String backpackType = getBackpackType(current);
            if (backpackType != null) {
                CuriosCompatImpl.tagBackpackDynamic(current, backpackType);
                player.sendActionBar(Component.text("Backpack ready for Curios slot", NamedTextColor.GOLD));
                Bukkit.getLogger().info("[ModularPacks-CuriosCompat] Tagged current backpack: " + backpackType);
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();

        if (item.hasItemMeta() && isBackpackItem(item)) {
            String backpackType = getBackpackType(item);
            if (backpackType != null) {
                CuriosCompatImpl.tagBackpackDynamic(item, backpackType);
            }
        }
    }

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
