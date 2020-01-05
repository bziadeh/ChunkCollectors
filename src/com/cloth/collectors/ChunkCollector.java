package com.cloth.collectors;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import com.cloth.inventory.InventoryCreator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.UUID;

/**
 * Created by Brennan on 1/4/2020.
 */
public class ChunkCollector implements Listener {

    private UUID factionLeader;

    private Location location;

    private Inventory inventory;

    public ChunkCollector(Player factionLeader, Location location) {
        this.factionLeader = factionLeader.getUniqueId();

        this.location = location;

        ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();

        // Creates the default inventory GUI for this collector.
        inventory = plugin.getInventoryCreator().getDefaultInventory();

        // Registers this class to ensure all event handlers execute.
        plugin.registerListener(this);
    }

    private static ItemStack collector;

    private static ItemMeta meta;

    static {
        collector = new ItemStack(Material.valueOf(Config.COLLECTOR_TYPE));

        meta = collector.getItemMeta();

        meta.setDisplayName(Config.COLLECTOR_TITLE);
    }

    /**
     * Gets the inventory/placeable item.
     *
     * @return the chunk collector ItemStack.
     */
    public static ItemStack getCollectorItem() {
        return collector;
    }

    /**
     * Gets the faction leader associated with this collector.
     *
     * @return the faction leader's UUID.
     */
    public UUID getFactionLeader() {
        return factionLeader;
    }

    /**
     * Gets the location the chunk collector was placed.
     *
     * @return the location.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Handles when the collector is clicked.
     *
     * @param event the PlayerInteractEvent.
     */
    @EventHandler
    public void onCollectorInteract(PlayerInteractEvent event) {
        if(isThisCollector(event.getClickedBlock()) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            event.getPlayer().openInventory(inventory);
        }
    }

    /**
     * Executed when a player breaks the chunk collector.
     *
     * @param event the BlockBreakEvent.
     */
    @EventHandler
    public void onCollectorBreak(BlockBreakEvent event) {
        if(isThisCollector(event.getBlock())) {
            destroy();
        }
    }

    /**
     * Executed when the chunk collector explodes.
     *
     * @param event the BlockExplodeEvent.
     */
    @EventHandler
    public void onCollectorExplode(BlockExplodeEvent event) {
        if(isThisCollector(event.getBlock())) {
            destroy();
        }
    }

    /**
     * Checks if the block is this chunk collector.
     *
     * @param block the block being checked.
     * @return if this block is this chunk collector.
     */
    private boolean isThisCollector(Block block) {
        return block != null && block.getLocation().equals(location);
    }

    /**
     * Executes when the chunk collector is destroyed (by a player or an explosion)
     */
    private void destroy() {
        ChunkCollectorPlugin.getInstance().getCollectorHandler().removeCollector(this);
    }
}
