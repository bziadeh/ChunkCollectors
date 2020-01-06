package com.cloth.collectors;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import com.cloth.config.SQLConnector;
import com.massivecraft.factions.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Brennan on 1/4/2020.
 */

public class CollectorHandler implements Listener {

    private List<ChunkCollector> collectorList;

    public CollectorHandler() {
        collectorList = new ArrayList<>();

        ChunkCollectorPlugin.getInstance().registerListener(this);
    }

    /**
     * Adds the chunk collector to the collectList, does not add it to the database.
     * The start() method will handle that.
     *
     * @param collector the collector that was placed.
     */
    public void addCollector(ChunkCollector collector) {
        collectorList.add(collector);
    }

    /**
     * Removes the collector from memory.
     *
     * @param collector the collector being removed.
     */
    public void removeCollector(ChunkCollector collector) {
        Inventory inventory = collector.getInventory().get();

        // Close player's inventory if the collector is destroyed. (VERY IMPORTANT, PREVENTS DUPING)
        for(int i = inventory.getViewers().size() - 1; i >= 0; i--) {
            inventory.getViewers().get(i).closeInventory();
        }

        ChunkCollectorPlugin.getInstance().unregisterListener(collector);
        collectorList.remove(collector);
    }

    /**
     * Returns a read-only list of the chunk collectors.
     *
     * @return the collectors that are active on the server.
     */
    public List<ChunkCollector> getCollectorList() {
        return Collections.unmodifiableList(collectorList);
    }

    /**
     * TEMPORARY: Starts the chunk collector 'save runnable'. Saves all cached collectors
     * to the SQLite database every 10 minutes.
     */
    public CollectorHandler start() {
        final int minutes = 10;
        final int time = 20 * 60 * minutes;

        new BukkitRunnable() {
            @Override
            public void run() {
                collectorList.forEach(SQLConnector::saveCollector);
            }
        }.runTaskTimerAsynchronously(ChunkCollectorPlugin.getInstance(), time, time);

        return this;
    }

    /**
     * Executed when someone places a chunk collector.
     *
     * @param event the BlockPlaceEvent.
     */
    @EventHandler
    public void onCollectorPlace(BlockPlaceEvent event) {

        final Player player = event.getPlayer();

        // Create the item we will compare to the default collector item...
        ItemStack itemInHandCompare = event.getItemInHand();
        itemInHandCompare.setAmount(1);

        // Check if the player is placing a chunk collector.
        if(itemInHandCompare.equals(ChunkCollector.getCollectorItem())) {
            FPlayer factionPlayer;

            // Is the player allowed to place a collector?
            if(!(factionPlayer = FPlayers.getInstance().getByPlayer(player)).hasFaction()) {
                event.setCancelled(true);
                player.sendMessage("you must be in a faction to place!");
                return;
            }

            Faction factionAt = Board.getInstance()
                    .getFactionAt(new FLocation(event.getBlockPlaced().getLocation()));

            // Does the player own the land where the collector is being placed?
            if(factionAt != null) {
                if(!factionAt.equals(factionPlayer.getFaction())) {
                    event.setCancelled(true);
                    player.sendMessage("cannot place here!");
                    return;
                }
            }

            // Yay, it succeeded!
            player.sendMessage(Config.COLLECTOR_PLACE);
            addCollector(new ChunkCollector(player, event.getBlockPlaced().getLocation(), "mob_collector"));
        }
    }
}
