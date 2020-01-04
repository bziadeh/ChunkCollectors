package com.cloth.inventory;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Created by Brennan on 1/4/2020.
 */
public class InventoryCreator {

    private Inventory inv;

    public InventoryCreator() {
        setupDefaults();
    }

    /**
     * Gets the default collector inventory.
     *
     * @return the default inventory.
     */
    public Inventory getDefaultInventory() {
        return inv;
    }

    /**
     * Sets up the default collector inventory.
     */
    private void setupDefaults() {
        inv = Bukkit.createInventory(null, Config.INVENTORY_SIZE, Config.COLLECTOR_TITLE);

        ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();

        for(String element : plugin.getConfig().getConfigurationSection("chunkcollector").getKeys(false)) {
            ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte)3);
            SkullMeta meta = (SkullMeta) item.getItemMeta();

            meta.setOwner("MHF_" + element.toUpperCase());
            item.setItemMeta(meta);

            int slot = plugin.getConfig().getInt("chunkcollector." + element + ".slot");
            inv.setItem(slot, item);
        }
    }
}
