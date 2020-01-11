package com.cloth.collectors;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import com.cloth.config.SQL;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Role;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
     * Adds the chunk collector to the collectList, and to the database.
     *
     * @param collector the collector that was placed.
     */
    public void addCollector(ChunkCollector collector) {
        collectorList.add(collector);

        SQL.saveCollector(collector);
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

        deleteHologramIfExists(collector);

        ChunkCollectorPlugin.getInstance().unregisterListener(collector);

        collectorList.remove(collector);

        SQL.removeCollector(collector);
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
     * Executed when someone places a chunk collector.
     *
     * @param event the BlockPlaceEvent.
     */
    @EventHandler
    public void onCollectorPlace(BlockPlaceEvent event) {

        final Player player = event.getPlayer();

        // Prevents NullPointerExceptions when calling isChunkCollector()
        if(event.getItemInHand() == null
                || event.getItemInHand().getItemMeta() == null
                || event.getItemInHand().getItemMeta().getDisplayName() == null) {
            return;
        }

        // Check if the player is placing a chunk collector.
        if(isChunkCollector(event.getItemInHand())) {
            FPlayer factionPlayer;

            // Is the player allowed to place a collector?
            if(!(factionPlayer = FPlayers.getInstance().getByPlayer(player)).hasFaction()) {
                event.setCancelled(true);
                player.sendMessage(Config.MUST_HAVE_FACTION);
                return;
            }

            Faction factionAt = Board.getInstance()
                    .getFactionAt(new FLocation(event.getBlockPlaced().getLocation()));

            if(factionAt == null) {
                return;
            }

            // Does the player own the land where the collector is being placed?
            if (!factionAt.equals(factionPlayer.getFaction())) {
                event.setCancelled(true);
                player.sendMessage(Config.COLLECTOR_PLACE_FAILURE);
                return;
            }

            int rank;

            if(factionPlayer.getRole().value < (rank = Config.PLACE_COLLECTOR_RANK)) {
                event.setCancelled(true);
                player.sendMessage(Config.COLLECTOR_DENY.replaceAll("%rank%", Role.getByValue(rank).nicename));
                return;
            }

            ChunkCollector collectorAtLocation;

            if((collectorAtLocation = getCollectorAtLocation(event.getBlockPlaced().getLocation())) != null) {
                event.setCancelled(true);
                String itemName = Config.COLLECTOR_ITEM_NAMES.get(collectorAtLocation.getType());
                String chunkOccupied = Config.CHUNK_OCCUPIED.replaceAll("%type%", itemName);
                player.sendMessage(chunkOccupied);
                return;
            }

            String type = null;
            Map<String, String> collectorItemNames = Config.COLLECTOR_ITEM_NAMES;

            for(String values : collectorItemNames.keySet()) {
                if(collectorItemNames.get(values).replaceAll("&", "§")
                        .equalsIgnoreCase(event.getItemInHand().getItemMeta().getDisplayName()))
                    type = values;
            }

            // The collector's type could not be identified?
            if(type == null) {
                System.out.println("Unable to identify collector type...");
                return;
            }

            // Yay, it succeeded!
            player.sendMessage(Config.COLLECTOR_PLACE.replaceAll("%type%",
                    Config.COLLECTOR_ITEM_NAMES.get(type).replaceAll("&", "§")));
            addCollector(new ChunkCollector(factionPlayer.getFaction(), event.getBlockPlaced().getLocation(), type));
        }
    }

    private void deleteHologramIfExists(ChunkCollector collector) {
        Hologram hologram;

        if((hologram = collector.getHologram()) != null) {
            hologram.delete();
        }
    }

    private boolean isChunkCollector(ItemStack collector) {
        for(String value : Config.COLLECTOR_ITEM_NAMES.values()) {
            if (collector.getItemMeta().getDisplayName().equalsIgnoreCase(value.replaceAll("&", "§")))
                return true;
        }
        return false;
    }

    public ChunkCollector getCollectorAtLocation(Location location) {
        for(ChunkCollector chunkCollector : collectorList) {
            if(chunkCollector.getLocation().getChunk().equals(location.getChunk())) {
                return chunkCollector;
            }
        }
        return null;
    }
}
