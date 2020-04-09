package com.cloth.inventory;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.collectors.ChunkCollector;
import com.cloth.config.Config;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.struct.Role;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Created by Brennan on 1/5/2020.
 */
public class CollectorInventoryHandler implements Listener {

    public CollectorInventoryHandler() {
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

            if((itemClicked = event.getCurrentItem()) == null) {
                return;
            }

            Player player = (Player) event.getWhoClicked();

            if(chunkCollector.getInventory().isCollecting(itemClicked.getType())) {

                if(event.getClick() == ClickType.RIGHT) {
                    chunkCollector.extract(itemClicked.getType(), player);
                    return;
                }

                if(event.getClick() == ClickType.LEFT) {
                    chunkCollector.sell(itemClicked.getType(), player);
                }
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
