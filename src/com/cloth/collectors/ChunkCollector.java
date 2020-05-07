package com.cloth.collectors;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import com.cloth.objects.CollectorInventory;
import com.cloth.objects.ItemData;
import com.cloth.objects.SafeBlock;
import com.cloth.util.LocationUtility;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.event.FactionDisbandEvent;
import com.massivecraft.factions.event.LandUnclaimAllEvent;
import com.massivecraft.factions.event.LandUnclaimEvent;
import com.massivecraft.factions.struct.Role;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Created by Brennan on 1/4/2020.
 */
public class ChunkCollector implements Listener {

    private transient Location location;

    private String locationString;

    private String factionId;

    private transient CollectorInventory inventory;

    private String inventoryBase64;

    private HashMap<Material, Integer> itemCollection;

    private String type;

    private transient Hologram hologram;

    private static Random random = new Random();

    private long lastHologramUpdate;

    public ChunkCollector(String factionId, Location location, String type) {
        this.factionId = factionId;

        this.locationString = LocationUtility.convertLocationToString(location);

        this.type = type;

        this.lastHologramUpdate = -1;

        ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();

        setupEmptyItemCollection();

        setupHologramIfEnabled();

        // Registers this class to ensure all event handlers execute.
        plugin.registerListener(this);

        updateItems();
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
        return Factions.getInstance().getFactionById(factionId);
    }

    /**
     * Sells all of the collected material.
     *
     * @param material the material being sold.
     * @param player the player selling the material.
     */
    public void sell(Material material, Player player) {
        int amountOf = getItemCollection().get(material);

        double pricePer = getInventory().getPrice(material);

        // Why would we let the player sell zero?
        if(amountOf == 0)
            return;

        ChunkCollectorPlugin.economy.depositPlayer(player, amountOf * pricePer);

        getItemCollection().put(material, 0);

        update(true);

        // Send success message:
        player.sendMessage(Config.COLLECTOR_SELL.replaceAll("&", "§")
                .replaceAll("%name%", getInventory().getName(material))
                .replaceAll("%amount%", String.valueOf(amountOf))
                .replaceAll("%price%", String.format("%,.2f", pricePer * amountOf)));
    }

    /**
     * Handles when the collector is clicked.
     *
     * @param event the PlayerInteractEvent.
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onCollectorInteract(PlayerInteractEvent event) {
        if(isThisCollector(event.getClickedBlock()) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            Player player = event.getPlayer();

            FPlayer fp = FPlayers.getInstance().getByPlayer(player);

            // Someone outside of the faction trying to sell the contents...
            if(!fp.hasFaction() || !fp.getFaction().equals(getFaction())) {
                return;
            }

            int rank;

            if(fp.getRole().value < (rank = Config.SELL_COLLECTOR_RANK)) {
                player.sendMessage(Config.COLLECTOR_DENY.replaceAll("%rank%", Role.getByValue(rank).nicename));
                return;
            }

            event.getPlayer().openInventory(getInventory().get());
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
            if(block.getLocation().getChunk().equals(getLocation().getChunk())) {
                ItemStack[] drops = block.getDrops().toArray(new ItemStack[0]);
                for(int i = drops.length - 1; i >= 0; i--) {
                    ItemStack drop = drops[i];
                    Material type = drop.getType();

                    // Are we collecting the item being broken?
                    if(getInventory().isCollecting(type)) {
                        event.getBlock().setType(Material.AIR);
                        getItemCollection().put(type, getItemCollection().get(type) + drop.getAmount());
                        update(false);
                    }
                }
            }
            return;
        }

        event.setCancelled(true);

        FPlayer player;

        if((player = FPlayers.getInstance().getByPlayer(event.getPlayer())).hasFaction()) {

            if(!player.getFaction().equals(getFaction())) {
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

        // Prevents cactus from growing.
        if(type == Material.CACTUS && Config.DISABLE_NATURAL_FARMS) {
            event.setCancelled(true);
        }

        // Checking if the block is cactus or sugarcane.
        if(type == Material.SUGAR_CANE || type == Material.CACTUS) {
            if(chunk.equals(getLocation().getChunk())) {
                if(getInventory().isCollecting(type)) {
                    // Cancel the grow event and collect the item. Improves TPS.
                    if(!event.isCancelled()) {
                        event.setCancelled(true);
                    }
                    // Collect...
                    getItemCollection().put(type, getItemCollection().get(type) + 1);
                    update(false);
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
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        // This event should only be used if FALL_DEATH mode is on. Otherwise, mobs are collected instantly
        // upon spawning, and we use the SpawnerSpawnEvent instead.
        if(!Config.COLLECTOR_MODE.equalsIgnoreCase("FALL_DEATH")) {
            return;
        }

        if(event.getEntity() instanceof Player) {
            return;
        }

        if(event.getEntity().getLocation().getChunk().equals(getLocation().getChunk())){

            boolean wildStacker = ChunkCollectorPlugin.isWildStackerInstalled;

            // TNT bank support for EntityDeathEvent.
            if(event.getEntity().getType() == EntityType.CREEPER && Config.TNT_BANK_ENABLED) {
                int tntAmount = wildStacker ? 3 * WildStackerAPI.getEntityAmount(event.getEntity()) : 3;
                getFaction().addTnt(tntAmount);
                event.getDrops().clear();
                return;
            }

            for(ItemStack drop : event.getDrops()) {
                Material type;
                if (getInventory().isCollecting((type = drop.getType()))) {
                    int amount = drop.getAmount();

                    if(wildStacker) {
                        amount *= WildStackerAPI.getEntityAmount(event.getEntity());
                    }

                    getItemCollection().put(type, getItemCollection().get(type) + amount);
                    update(false);
                }
            }

            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if(!Config.COLLECTOR_MODE.equalsIgnoreCase("INSTANT")) {
            return;
        }

        if(event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }

        if(event.getLocation().getChunk().equals(getLocation().getChunk())){

            event.setCancelled(true);

            int amount = 1;

            if(ChunkCollectorPlugin.isWildStackerInstalled) {
                amount *= WildStackerAPI.getEntityAmount(event.getEntity());
            }

            processDrops(event.getEntityType(), amount);
            update(false);
        }
    }

    @EventHandler
    public void onFactionDisband(FactionDisbandEvent event) {
        List<ChunkCollector> collectors = ChunkCollectorPlugin.getInstance().getCollectorHandler().getCollectorList();

        for(int i = collectors.size() - 1; i >= 0; i--) {
            ChunkCollector collector;
            if((collector = collectors.get(i)).getFaction().getId().equalsIgnoreCase(event.getFaction().getId())) {
                collector.destroy(collector.getLocation(), true);
            }
        }
    }

    /**
     * Destroys this collector if the land which it is placed on is unclaimed.
     *
     * @param event the LandUnclaimEvent.
     */
    @EventHandler
    public void onUnclaim(LandUnclaimEvent event) {
        if(getLocation().getChunk().equals(event.getLocation().getChunk())) {
            destroy(getLocation(), true);
        }
    }

    /**
     * Checks if the block is this chunk collector.
     *
     * @param block the block being checked.
     * @return if this block is this chunk collector.
     */
    private boolean isThisCollector(Block block) {
        return block != null && block.getLocation().equals(getLocation());
    }

    /**
     * Executes when the chunk collector is destroyed (by a player or an explosion)
     */
    public void destroy(Location location, boolean drop) {
        if(drop) {
            location.getBlock().setType(Material.AIR);
            location.getWorld().dropItem(location, ChunkCollector.getCollectorItem(type));
        }
        ChunkCollectorPlugin.getInstance().getCollectorHandler().removeCollector(this);
    }

    /**
     * Updates the item's in the GUI.
     */
    public void update(boolean bypassDelay) {
        updateItems();

        if(getHologram() != null) {
            // Has at least one second passed since the last hologram update? (prevents flickering)
            if(bypassDelay || lastHologramUpdate == -1 || ((System.currentTimeMillis() - lastHologramUpdate) / 1000) > 1) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(getHologram().size() > 1) {
                            getHologram().removeLine(1);
                            getHologram().insertTextLine(1, "§a$" + String.format("%,.2f", getTotalWorth()));
                            lastHologramUpdate = System.currentTimeMillis();
                        }
                    }
                }.runTask(ChunkCollectorPlugin.getInstance());
            }
        }
    }

    private void updateItems() {
        for(Material material : getInventory().getCollectedMaterials()) {

            ItemStack item = getInventory().get().getItem(getInventory().getSlot(material)); // the current item...
            ItemMeta meta = item.getItemMeta();
            ItemData itemData = getInventory().getItemData(material);

            meta.setDisplayName(itemData.getName().replaceAll("%amount%", String.valueOf(getItemCollection().get(material))));
            item.setItemMeta(meta);

            getInventory().get().getViewers().forEach(viewer -> ((Player) viewer).updateInventory());
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

        for(Material material : getItemCollection().keySet()) {
            if(getInventory().isCollecting(material)) {
                total += getItemCollection().get(material) * getInventory().getPrice(material);
            }
        }

        return total;
    }

    /**
     * The itemCollection keeps track of what items we are collecting, and how many we have stored in the collector.
     * This method is executed when the collector is created, storing our materials and setting them to zero.
     */
    private void setupEmptyItemCollection() {
        getInventory().getCollectedMaterials().forEach(material -> getItemCollection().put(material, 0));
    }

    /**
     * Gets the chunk collector's inventory.
     *
     * @return the inventory.
     */
    public CollectorInventory getInventory() {
        if(inventory == null) {
            return inventory = new CollectorInventory(ChunkCollectorPlugin.getInstance().getInventoryCreator().getDefaultInventories().get(this.type));
        } else {
            return inventory;
        }
    }

    private void processDrops(EntityType entityType, int multiplier) {
        switch(entityType) {
            case IRON_GOLEM:
                add(6 * multiplier, Material.IRON_INGOT);
                break;
            case CREEPER:
                if(Config.TNT_BANK_ENABLED)
                    getFaction().addTnt(3 * multiplier);
                else
                    add(3 * multiplier, Material.TNT);
                break;
            case ZOMBIE:
                add(2 * multiplier, Material.ROTTEN_FLESH);
                break;
            case SKELETON:
                add(2 * multiplier, Material.BONE, Material.ARROW);
                break;
            case SPIDER:
                add(2 * multiplier, Material.STRING, Material.SPIDER_EYE);
                break;
            case PIG_ZOMBIE:
                add(2 * multiplier, Material.GOLD_INGOT, Material.GOLD_NUGGET);
                break;
            case SLIME:
                add(2 * multiplier, Material.SLIME_BALL);
                break;
            case ENDERMAN:
                add(2 * multiplier, Material.ENDER_PEARL);
                break;
            case BLAZE:
                add(2 * multiplier, Material.BLAZE_ROD);
                break;
            case PIG:
                add(2 * multiplier, Material.PORK);
                break;
            case VILLAGER:
                add(3 * multiplier, Material.EMERALD);
                break;
            case CHICKEN:
                add(2 * multiplier, Material.FEATHER, Material.RAW_CHICKEN);
                break;
            case COW:
                add(2 * multiplier, Material.RAW_BEEF, Material.LEATHER);
                break;
            case SHEEP:
                add(2 * multiplier, Material.WOOL, Material.MUTTON);
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
            if(getItemCollection().containsKey(material)) {
                getItemCollection().put(material, getItemCollection().get(material) + random.nextInt(max) + 1);
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
     * Gets how much of the specified material is currently stored in the
     * chunk collector.
     *
     * @param material the material being checked.
     * @return the amount stored in the collector.
     */
    public int getAmountOf(Material material) {
        return getItemCollection().get(material);
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
        Location location = getLocation();
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

            getItemCollection().put(material, amount);
        }

        update(true);
    }

    /**
     * Extracts the material into the player's inventory.
     *
     * @param material the material being extracted.
     * @param player the player being given the material.
     */
    public void extract(Material material, Player player) {

        int total = 0;

        while(player.getInventory().firstEmpty() != -1 && getItemCollection().get(material) > 0) {
            int amount = getItemCollection().get(material);
            ItemStack itemToGive;

            // Does the collector have more than one stack?
            if(amount > 64) {
                itemToGive = new ItemStack(material, 64);
                getItemCollection().put(material, amount - 64);
            } else {
                itemToGive = new ItemStack(material, amount);
                getItemCollection().put(material, 0);
            }

            player.getInventory().addItem(itemToGive);
            total += itemToGive.getAmount();
        }

        if(total != 0) {
            // Send extract message.
            player.sendMessage(Config.COLLECTOR_EXTRACT
                    .replaceAll("%name%", getInventory().getName(material))
                    .replaceAll("%amount%", String.valueOf(total)));

            // Update the inventory after extracting.
            update(true);
        }
    }

    private void setupHologramIfEnabled() {
        ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();

        if(Config.COLLECTOR_HOLOGRAMS_ENABLED) {
            // We put this in a runnable to ensure it runs on the main thread.
            // We're not allowed to create holograms async, and our CollectorSQL loader is running
            // on another thread when loading and initializing these collectors.
            new BukkitRunnable() {
                @Override
                public void run() {
                    hologram = HologramsAPI.createHologram(plugin, getLocation().clone().add(0.5, 1.75, 0.5));
                    hologram.insertTextLine(0, Config.COLLECTOR_ITEM_NAMES.get(type).replaceAll("&", "§"));
                    hologram.insertTextLine(1, "§a$" + String.format("%,.2f", getTotalWorth()));
                    hologram.insertTextLine(2, "");
                }
            }.runTask(plugin);
        }
    }

    public void setInventoryBase64(String inventoryBase64) {
        this.inventoryBase64 = inventoryBase64;
    }

    public String getInventoryBase64() {
        return inventoryBase64;
    }

    public HashMap<Material, Integer> getItemCollection() {
        return itemCollection == null ? itemCollection = new HashMap<>() : itemCollection;
    }

    public Hologram getHologram() {
        if(hologram == null) {
            setupHologramIfEnabled();
        }
        return hologram;
    }

    public Location getLocation() {
        if(location == null) {
            location = LocationUtility.convertStringToLocation(locationString);
        }
        return location;
    }
}