package com.cloth.collectors;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import com.cloth.objects.CollectorInventory;
import com.cloth.objects.ItemData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Brennan on 1/4/2020.
 */
public class ChunkCollector implements Listener {

    private UUID factionLeader;

    private Location location;

    private CollectorInventory inventory;

    private HashMap<Material, Integer> itemCollection;

    private Random random;

    public ChunkCollector(Player factionLeader, Location location, String type) {
        random = new Random();

        itemCollection = new HashMap<>();

        this.factionLeader = factionLeader.getUniqueId();

        this.location = location;

        ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();

        // Creates the default inventory GUI for this collector.
        inventory = new CollectorInventory(plugin.getInventoryCreator().getDefaultInventories().get(type));

        setupEmptyItemCollection();

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
     * Sells all of the collected material.
     *
     * @param material the material being sold.
     * @param player the player selling the material.
     */
    public void sell(Material material, Player player) {
        int amountOf = itemCollection.get(material);

        double pricePer = getInventory().getPrice(material);

        // Why would we let the player sell zero?
        if(amountOf == 0)
            return;

        ChunkCollectorPlugin.economy.depositPlayer(player, amountOf * pricePer);

        itemCollection.put(material, 0);

        update();

        // Send success message:
        player.sendMessage(Config.COLLECTOR_SELL.replaceAll("&", "§")
                .replaceAll("%name%", getInventory().getName(material))
                .replaceAll("%amount%", String.valueOf(amountOf))
                .replaceAll("%price%", String.valueOf(pricePer * amountOf)));
    }

    /**
     * Handles when the collector is clicked.
     *
     * @param event the PlayerInteractEvent.
     */
    @EventHandler
    public void onCollectorInteract(PlayerInteractEvent event) {
        if(event.getPlayer().isSneaking()) {
            return;
        }

        if(isThisCollector(event.getClickedBlock()) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            // check if same faction (& faction role for permission)
            event.getPlayer().openInventory(inventory.get());
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
            if(!event.isCancelled()) {
                event.setCancelled(true);
                destroy(event.getBlock().getLocation(), true);

                // check faction role for permission?
                event.getPlayer().sendMessage(Config.COLLECTOR_BREAK);
            }
        }
    }

    /**
     * Executed when the chunk collector explodes.
     *
     * @param event the BlockExplodeEvent.
     */
    @EventHandler
    public void onCollectorExplode(EntityExplodeEvent event) {
        for(Block block : event.blockList()) {
            if(!event.isCancelled()) {
                if(isThisCollector(block)) {
                    destroy(block.getLocation(), false);
                }
            }
        }
    }

    /**
     *  Executes when an entity dies within the chunk.
     *
     * @param event the EntityDeathEvent.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if(event.getEntity().getLocation().getChunk().equals(location.getChunk())){

            // This event should only be used if FALL_DEATH mode is on. Otherwise, mobs are collected instantly
            // upon spawning, and we use the SpawnerSpawnEvent instead.
            if(!Config.COLLECTOR_MODE.equalsIgnoreCase("FALL_DEATH")) {
                return;
            }

            for(ItemStack drop : event.getDrops()) {
                Material type;
                if (inventory.isCollecting((type = drop.getType()))) {
                    itemCollection.put(type, itemCollection.get(type) + drop.getAmount());
                    update();
                }
            }
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onEntitySpawn(SpawnerSpawnEvent event) {
        if(event.getEntity().getLocation().getChunk().equals(location.getChunk())){
            if(!Config.COLLECTOR_MODE.equalsIgnoreCase("INSTANT")) {
                return;
            }

            event.setCancelled(true);
            processDrops(event.getEntityType());
            update();
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
    private void destroy(Location location, boolean drop) {
        if(drop)
            location.getBlock().breakNaturally(ChunkCollector.getCollectorItem());
        ChunkCollectorPlugin.getInstance().getCollectorHandler().removeCollector(this);
    }

    /**
     * Updates the item's in the GUI.
     */
    public void update() {
        for(Material material : inventory.getCollectedMaterials()) {

            ItemStack item = inventory.get().getItem(inventory.getSlot(material)); // the current item...
            ItemMeta meta = item.getItemMeta();
            ItemData itemData = inventory.getItemData(material);

            meta.setDisplayName(itemData.getName() + " §8(§7" + itemCollection.get(material) + "§8)");
            item.setItemMeta(meta);

            inventory.get().getViewers().forEach(viewer -> ((Player) viewer).updateInventory());
        }
    }

    /**
     * The itemCollection keeps track of what items we are collecting, and how many we have stored in the collector.
     * This method is executed when the collector is created, storing our materials and setting them to zero.
     */
    private void setupEmptyItemCollection() {
        inventory.getCollectedMaterials().forEach(material -> itemCollection.put(material, 0));
    }

    /**
     * Gets the chunk collector's inventory.
     *
     * @return the inventory.
     */
    public CollectorInventory getInventory() {
        return inventory;
    }

    private void processDrops(EntityType entityType) {
        switch(entityType) {
            case IRON_GOLEM:
                add(6, Material.IRON_INGOT);
                break;
            case CREEPER:
                add(3, Material.SULPHUR);
                break;
            case ZOMBIE:
                add(2, Material.ROTTEN_FLESH);
                break;
            case SKELETON:
                add(2, Material.BONE, Material.ARROW);
                break;
            case SLIME:
                add(2, Material.SLIME_BALL);
                break;
            case ENDERMAN:
                add(2, Material.ENDER_PEARL);
                break;
            case BLAZE:
                add(2, Material.BLAZE_ROD);
                break;
            case PIG:
                add(2, Material.PORK);
                break;
        }
    }

    /**
     * Checks if the collector is collecting these materials, and if so
     * adds them to the item collection.
     *
     * @param max the maximum amount of items that can be added (# generated randomly)
     * @param materials the materials being added to the collector.
     */
    private void add(int max, Material... materials) {
        for(Material material : materials) {
            if(itemCollection.containsKey(material)) {
                itemCollection.put(material, itemCollection.get(material) + random.nextInt(max) + 1);
            }
        }
    }
}
