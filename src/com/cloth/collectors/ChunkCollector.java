package com.cloth.collectors;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import com.cloth.objects.CollectorInventory;
import com.cloth.objects.ItemData;
import com.cloth.objects.SafeBlock;
import com.cloth.packets.PacketHandler;
import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Role;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockAction;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Created by Brennan on 1/4/2020.
 */
public class ChunkCollector extends SafeBlock implements Listener {

    private Faction faction;

    private Location location;

    private CollectorInventory inventory;

    private HashMap<Material, Integer> itemCollection;

    private Random random;

    private String type;

    public ChunkCollector(Faction faction, Location location, String type) {
        super(location);

        random = new Random();

        itemCollection = new HashMap<>();

        this.faction = faction;

        this.location = location;

        this.type = type;

        ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();

        inventory = new CollectorInventory(plugin.getInventoryCreator().getDefaultInventories().get(this.type = type));

        setupEmptyItemCollection();

        // Registers this class to ensure all event handlers execute.
        plugin.registerListener(this);
    }

    private static ItemStack collector;

    private static ItemMeta meta;

    static {
        collector = new ItemStack(Material.valueOf(Config.COLLECTOR_TYPE));

        meta = collector.getItemMeta();

        List<String> lore = Config.COLLECTOR_ITEM_LORE;

        // Update the placeholder values in the configuration file.
        for(int i = lore.size() - 1; i >= 0; i--) {
            lore.set(i, lore.get(i)
                    .replaceAll("&", "§"));
        }

        meta.setLore(lore);

        collector.setItemMeta(meta);
    }

    /**
     * Gets the inventory/placeable item.
     *
     * @return the chunk collector ItemStack.
     */
    public static ItemStack getCollectorItem(String type) {
        ItemMeta meta = collector.getItemMeta();

        meta.setDisplayName(Config.COLLECTOR_ITEM_NAMES.get(type).replaceAll("&", "§"));

        collector.setItemMeta(meta);

        return collector;
    }

    /**
     * Gets the faction associated with this collector.
     *
     * @return the faction leader's UUID.
     */
    public Faction getFaction() {
        return faction;
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
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onCollectorInteract(PlayerInteractEvent event) {
        boolean wasCancelled = event.isCancelled();

        if(event.getPlayer().isSneaking()) {
            return;
        }

        if(isThisCollector(event.getClickedBlock()) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            Player player = event.getPlayer();

            FPlayer fp = FPlayers.getInstance().getByPlayer(player);

            // Someone outside of the faction trying to sell the contents...
            if(!fp.hasFaction() || !fp.getFaction().equals(faction)) {
                return;
            }

            int rank;

            if(fp.getRole().value < (rank = Config.SELL_COLLECTOR_RANK)) {
                player.sendMessage(Config.COLLECTOR_DENY.replaceAll("%rank%", Role.getByValue(rank).nicename));
                return;
            }

            // Remove the chat packet sent by Factions.
            if(wasCancelled) {
                PacketHandler.playersToRemove.add(player.getName());
            }

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
            event.setCancelled(true);

            FPlayer player;

            if((player = FPlayers.getInstance().getByPlayer(event.getPlayer())).hasFaction()) {

                if(!player.getFaction().equals(faction)) {
                    return;
                }

                int rank;

                if(player.getRole().value < (rank = Config.DESTROY_COLLECTOR_RANK)) {
                    player.sendMessage(Config.COLLECTOR_DENY.replaceAll("%rank%", Role.getByValue(rank).nicename));
                    return;
                }

                destroy(event.getBlock().getLocation(), true);

                event.getPlayer().sendMessage(Config.COLLECTOR_BREAK.replaceAll("%type%",
                        Config.COLLECTOR_ITEM_NAMES.get(type).replaceAll("&", "§")));
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
        if(drop) {
            location.getBlock().setType(Material.AIR);
            location.getWorld().dropItemNaturally(location, ChunkCollector.getCollectorItem(type));
        }
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

    /**
     * Gets the type of this collector.
     *
     * @return the type.
     */
    public String getType() {
        return type;
    }
}
