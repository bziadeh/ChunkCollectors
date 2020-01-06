package com.cloth.inventory;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.collectors.ChunkCollector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Created by Brennan on 1/5/2020.
 */
public class InventoryHandler implements Listener {

    public InventoryHandler() {
        ChunkCollectorPlugin.getInstance().registerListener(this);
    }

    /**
     * Executes when a player clicks an item in a chunk collector GUI.
     *
     * @param event the InventoryClickEvent.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getInventory() == null || event.getClickedInventory() == null) {
            return;
        }

        ChunkCollector chunkCollector;

        if((chunkCollector = isCollectorInventory(event.getClickedInventory())) != null) {
            event.setCancelled(true);

            // Did they click in the collector inventory, or their own?
            if(event.getRawSlot() != event.getSlot()) {
                return;
            }

            ItemStack itemClicked;

            // Is the clicked item null?
            if((itemClicked = event.getCurrentItem()) == null) {
                return;
            }

            if(chunkCollector.getInventory().isCollecting(itemClicked.getType())) {
                chunkCollector.sell(itemClicked.getType(), (Player) event.getWhoClicked());
            }
        }
    }

    /**
     * Checks if the inventory specified is a chunk collector inventory GUI.
     *
     * @param inventory the inventory being checked.
     * @return whether or not the inventory is a collector.
     */
    private ChunkCollector isCollectorInventory(Inventory inventory) {
        for(ChunkCollector chunkCollector : ChunkCollectorPlugin.getInstance().getCollectorHandler().getCollectorList()){
            if(chunkCollector.getInventory().get().equals(inventory)) {
                return chunkCollector;
            }
        }
        return null;
    }
}