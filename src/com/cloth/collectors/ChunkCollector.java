package com.cloth.collectors;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import com.cloth.objects.CollectorInventory;
import com.cloth.objects.ItemData;
import com.cloth.objects.SafeBlock;
import com.cloth.packets.PacketHandler;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Role;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockAction;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Crops;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

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

    private Hologram hologram;

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

        setupHologramIfEnabled();

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

        // Adding glow to our item...
        if(Config.COLLECTOR_ITEM_GLOWING) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

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

        Block block;

        // Checks if the block being broken is NOT a collector, and in the chunk.
        if(!isThisCollector((block = event.getBlock()))) {
            if(block.getLocation().getChunk().equals(location.getChunk())) {
                ItemStack[] drops = block.getDrops().toArray(new ItemStack[0]);
                for(int i = drops.length - 1; i >= 0; i--) {
                    ItemStack drop = drops[i];
                    Material type = drop.getType();

                    // Are we collecting the item being broken?
                    if(inventory.isCollecting(type)) {
                        event.getBlock().setType(Material.AIR);
                        itemCollection.put(type, itemCollection.get(type) + drop.getAmount());
                        update();
                    }
                }
            }
            return;
        }

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCropCrow(BlockGrowEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        Material type = event.getNewState().getType();

        // Converting SUGAR_CANE_BLOCK to SUGAR_CANE
        if(type == Material.SUGAR_CANE_BLOCK) {
            type = Material.SUGAR_CANE;
        }

        // Checking if the block is cactus or sugarcane.
        if(type == Material.SUGAR_CANE || type == Material.CACTUS) {
            if(chunk.equals(location.getChunk())) {
                if(inventory.isCollecting(type)) {
                    event.setCancelled(true);
                    itemCollection.put(type, itemCollection.get(type) + 1);
                    update();
                }
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
        if(!event.isCancelled()) {
            for(int i = event.blockList().size() - 1; i >= 0; i--) {
                Block block = event.blockList().get(i);
                if(isThisCollector(block)) {
                    event.blockList().remove(i);
                    destroy(block.getLocation(), true);
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
            location.getWorld().dropItem(location, ChunkCollector.getCollectorItem(type));
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

        if(hologram != null) {
            if(hologram.size() > 1) {
                hologram.removeLine(1);
                hologram.insertTextLine(1, "§a$" + getTotalWorth());
            }
        }
    }

    /**
     * Adds all the collected items and returns the
     * amount of money you would get if sold.
     *
     * @return the total amount of money.
     */
    public double getTotalWorth() {
        double total = 0;

        for(Material material : itemCollection.keySet()) {
            total += itemCollection.get(material) * inventory.getPrice(material);
        }

        return total;
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
            case SPIDER:
                add(2, Material.STRING, Material.SPIDER_EYE);
                break;
            case PIG_ZOMBIE:
                add(2, Material.GOLD_INGOT, Material.GOLD_NUGGET);
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

    /**
     * Gets the collector's hologram.
     *
     * @return the hologram.
     */
    public Hologram getHologram() {
        return hologram;
    }

    /**
     * Gets how much of the specified material is currently stored in the
     * chunk collector.
     *
     * @param material the material being checked.
     * @return the amount stored in the collector.
     */
    public int getAmountOf(Material material) {
        return itemCollection.get(material);
    }

    /**
     * Formats the contents of this collector so we can
     * easily read and write them to the SQLite database.
     *
     * @return the contents of the collector.
     */
    public String formatContents() {
        StringBuilder stringBuilder = new StringBuilder();

        Material[] materials = getInventory()
                .getCollectedMaterials().toArray(new Material[0]);

        for(int i = 0; i < materials.length; i++) {
            int amount = getAmountOf(materials[i]);

            if(i > 0) {
                stringBuilder.append(">");
            }

            stringBuilder.append(String.format("%s,%d", materials[i].name(), amount));
        }

        return stringBuilder.toString();
    }

    /**
     * Formats the location of this collector so we can
     * easily read and write it to the SQLite database.
     *
     * @return the formatted location.
     */
    public String formatLocation() {
        return String.format("%s,%.2f,%.2f,%.2f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ());
    }

    /**
     * Fills the collector's contents using a formatted string
     * taken from the database.
     *
     * @param contents the formatted string.
     */
    public void fill(String contents) {
       String[] sections = contents.split(">");

       for(String section : sections) {
           String[] data = section.split(",");

           Material material = Material.getMaterial(data[0]);

           int amount = Integer.parseInt(data[1]);

           itemCollection.put(material, amount);
       }

       update();
    }

    private void setupHologramIfEnabled() {
        ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();

        if(Config.COLLECTOR_HOLOGRAMS_ENABLED) {
            // We put this in a runnable to ensure it runs on the main thread.
            // We're not allowed to create holograms async, and our SQL loader is running
            // on another thread when loading and initializing these collectors.
            new BukkitRunnable() {
                @Override
                public void run() {
                    hologram = HologramsAPI.createHologram(plugin, location.clone().add(0.5, 1.25, 0.5));
                    hologram.insertTextLine(0, Config.COLLECTOR_ITEM_NAMES.get(type).replaceAll("&", "§"));
                    hologram.insertTextLine(1, "§a$" + getTotalWorth());
                    hologram.insertTextLine(2, "");
                    hologram.insertTextLine(3, "");
                    hologram.insertItemLine(4, new ItemStack(itemCollection.keySet().toArray(new Material[0])[0]));
                }
            }.runTask(plugin);
        }
    }
}
