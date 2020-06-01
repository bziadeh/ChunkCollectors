package com.cloth.inventory;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import com.cloth.objects.CollectorInventory;
import com.cloth.objects.ItemData;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Brennan on 1/4/2020.
 */
public class InventoryCreator {

    private Map<String, CollectorInventory> collectorInventories;

    private CountDownLatch latch;

    private final String user = "%%__USERNAME__%%";
    private final String version = "%%__VERSION__%%";
    private final String resource = "%%__RESOURCE__%%";
    private final String timestamp = "%%__TIMESTAMP__%%";

    public InventoryCreator(CountDownLatch latch) {
        this.latch = latch;

        collectorInventories = new HashMap<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                setupDefaults();
            }
        }.runTaskAsynchronously(ChunkCollectorPlugin.getInstance());
    }

    /**
     * Gets the default collector inventories.
     *
     * @return the default inventory.
     */
    public Map<String, CollectorInventory>  getDefaultInventories() {
        return collectorInventories;
    }

    /**
     * Sets the background color for a collector GUI.
     *
     * @param inventory the inventory being edited.
     * @param config the configuration file.
     * @param collector the name/type of the collector.
     */
    private void handleBackground(Inventory inventory, FileConfiguration config, String collector) {
        String backgroundColor = config.getString("collectors." + collector + ".background");

        if(backgroundColor.equalsIgnoreCase("none")) {
            return;
        }

        try {
            DyeColor dyeColor = DyeColor.valueOf(backgroundColor);

            // If the dye color is null, the color name entered was not found. So, we check...
            if(dyeColor != null) {
                ItemStack bgItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) dyeColor.ordinal());
                for(int i = 0; i < inventory.getSize(); i++) {
                    if(inventory.getItem(i) == null) {
                        inventory.setItem(i, bgItem);
                    }
                }
            }
        } catch (IllegalArgumentException error) {
            // The color specified in the configuration file was not found...
            System.out.println("Failed to set background color for collector: " + collector);
        }
    }

    /**
     * Converts all color codes in a list of strings.
     *
     * @param args path to the list
     * @return the list with translated color codes.
     */
    private List<String> getLore(String args) {
        ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();

        List<String> converted = new ArrayList<>();

        if(String.valueOf(plugin.getConfig().get(args))
                .equalsIgnoreCase("NONE")) {
            return converted;
        }

        plugin.getConfig().getList(args).forEach(message -> {
            converted.add(message.toString()
                    .replaceAll("&", "§"));
        });

        return converted;
    }

    /**
     * Sets up the default collector inventory.
     */
    private void setupDefaults() {
        ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();
        FileConfiguration config = plugin.getConfig();

        for(String collector : config.getConfigurationSection("collectors").getKeys(false)) {
            Inventory inv = Bukkit.createInventory(null, Config.INVENTORY_SIZE, Config.COLLECTOR_TITLE);

            CollectorInventory collectorInventory = new CollectorInventory();

            for(String drop : config.getConfigurationSection("collectors." + collector).getKeys(false)) {

                // Handles the background color for this collector.
                if(drop.equalsIgnoreCase("background")) {
                    handleBackground(inv, config, collector);
                    continue;
                }

                // Ensures the item-name option does not cause any problems.
                if(drop.equalsIgnoreCase("item-name")) {
                    continue;
                }

                String path = "collectors." + collector + "." + drop;
                int slot = config.getInt(path + ".slot");
                String name = config.getString(path + ".name").replaceAll("&", "§");
                Material material = Material.valueOf(drop);
                List<String> lore = getLore(path + ".lore");

                collectorInventory.set(material, new ItemData(config.getDouble(path + ".pricePer"), slot, name, lore));

                // Creating our custom drop item.
                ItemStack itemToCollect = new ItemStack(material);
                ItemMeta meta = itemToCollect.getItemMeta();

                meta.setDisplayName(name);
                meta.setLore(lore);

                itemToCollect.setItemMeta(meta);
                inv.setItem(slot, itemToCollect);
            }

            collectorInventory.setInventory(inv);
            collectorInventories.put(collector, collectorInventory);
        }

        // We're done with in this thread, countdown.
        latch.countDown();
    }
}
