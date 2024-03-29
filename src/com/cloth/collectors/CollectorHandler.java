package com.cloth.collectors;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import com.cloth.util.InventoryUtil;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.*;
import com.massivecraft.factions.event.FactionDisbandEvent;
import com.massivecraft.factions.event.LandUnclaimAllEvent;
import com.massivecraft.factions.struct.Role;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Brennan on 1/4/2020.
 */

public class CollectorHandler implements Listener {

    private static final SimpleDateFormat backupFormat;

    static {
        backupFormat = new SimpleDateFormat("hh mm ss");
    }

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

        // remove collector from database
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
     * Executed when a player disbands their faction. (or the last player leaves)
     *
     * @param event the FactionDisbandEvent.
     */
    @EventHandler
    public void onFactionDisband(FactionDisbandEvent event) {
        List<ChunkCollector> collectors = ChunkCollectorPlugin.getInstance().getCollectorHandler().getCollectorList();

        // Last player left...
        if(event.getFaction() == null) {
            removeNullCollectors();
            return;
        }

        // Normal disband using command...
        for(int i = collectors.size() - 1; i >= 0; i--) {
            ChunkCollector collector;
            if((collector = collectors.get(i)).getFaction().getId().equalsIgnoreCase(event.getFaction().getId())) {
                collector.destroy(true);
            }
        }
    }

    /**
     * Executed when a player unclaims all of their land.
     *
     * @param event the LandUnclaimAllEvent.
     */
    @EventHandler
    public void onUnclaimAll(LandUnclaimAllEvent event) {
        for(int i = collectorList.size() - 1; i >= 0; i--) {
            final ChunkCollector collector = collectorList.get(i);
            if(collector.getFaction().getId()
                    .equalsIgnoreCase(event.getFaction().getId())) {
                collector.destroy(true);
            }
        }
    }

    /**
     * Executed when someone places a chunk collector.
     *
     * @param event the BlockPlaceEvent.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
            if(!(factionPlayer = FPlayers.getInstance().getByPlayer(player)).hasFaction() && !factionPlayer.isAdminBypassing()) {
                event.setCancelled(true);
                player.sendMessage(Config.MUST_HAVE_FACTION);
                return;
            }

            Faction factionAt = Board.getInstance()
                    .getFactionAt(new FLocation(event.getBlockPlaced().getLocation()));

            if(factionAt == null) {
                return;
            }

            // Not allowed to place in SafeZone, WarZone, Wilderness
            if(factionAt.isSystemFaction()) {
                event.setCancelled(true);
                player.sendMessage(Config.COLLECTOR_PLACE_FAILURE);
                return;
            }

            // Does the player own the land where the collector is being placed?
            if (!factionAt.equals(factionPlayer.getFaction()) && !factionPlayer.isAdminBypassing()) {
                event.setCancelled(true);
                player.sendMessage(Config.COLLECTOR_PLACE_FAILURE);
                return;
            }

            int rank;

            if(factionPlayer.getRole().value < (rank = Config.PLACE_COLLECTOR_RANK) && !factionPlayer.isAdminBypassing()) {
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
            addCollector(new ChunkCollector(factionAt.getId(), event.getBlockPlaced().getLocation(), type));
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

    public ChunkCollector getCollectorAtLocation(Chunk chunk) {
        return collectorList.stream().filter(collector -> collector.getLocation().getChunk().equals(chunk))
                .findFirst().orElse(null);
    }

    /**
     * Creates a backup JSON of all saved collectors.
     */
    public void backup() {
        final ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();

        File file = new File(plugin.getDataFolder() + "/backups");

        if(!file.exists()) {
            file.mkdirs();
        }

        final String time = backupFormat.format(new Date());
        final String fileName = String.format("%s.json", time);

        saveAll("backups/" + fileName);
    }

    /**
     * Loads all collectors in from JSON.
     *
     * @param path the path to the file.
     */
    public void loadAll(String path) {
        final ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();
        try (Reader reader = new FileReader(plugin.getDataFolder() + "/" + path)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<ChunkCollector>>(){}.getType();
            collectorList = gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        collectorList.forEach(collector -> {
            plugin.registerListener(collector);
            collector.update(true);
        });
    }

    /**
     * Saves all collectors to JSON.
     *
     * @param path the path to the file.
     */
    public void saveAll(String path) {
        final ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();
        collectorList.forEach(collector -> {
            collector.setInventoryBase64(InventoryUtil.toBase64(collector.getInventory().get()));
        });
        try (Writer writer = new FileWriter(plugin.getDataFolder() + "/" + path)) {
            Gson gson = new Gson();
            gson.toJson(collectorList, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes any collector with an empty faction from memory.
     */
    private void removeNullCollectors() {
        for(int i = collectorList.size() - 1; i >= 0; i--) {
            ChunkCollector collector = collectorList.get(i);
            if(collector.getFaction().getFPlayers().isEmpty()) {
                collector.destroy(true);
            }
        }
    }
}
