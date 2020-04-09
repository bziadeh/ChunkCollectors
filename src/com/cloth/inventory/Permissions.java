package com.cloth.inventory;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.config.Config;
import com.cloth.objects.CFancyItem;
import com.massivecraft.factions.struct.Role;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Created by Brennan on 1/11/2020.
 */
public class Permissions implements Listener {

    private static Inventory inventory;

    private static ItemStack breakItem;

    private static ItemStack placeItem;

    private static ItemStack useItem;

    private static String required;

    private static String cycle;

    static {
        required = "§cRequired Rank §f";

        cycle = "§rLeft-click to cycle through the options.";

        createItems();
    }

    public Permissions() {
        setupGUI();

        ChunkCollectorPlugin.getInstance().registerListener(this);
    }

    /**
     * Gets the permissions inventory.
     *
     * @return the inventory.
     */
    public static Inventory getGUI() {
        return inventory;
    }

    /**
     * Creates the permissions inventory and adds a background.
     */
    private void setupGUI() {
        inventory = Bukkit.createInventory(null, 27, "Permissions");

        inventory.setItem(11, breakItem);

        inventory.setItem(13, placeItem);

        inventory.setItem(15, useItem);

        ItemStack bgItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) DyeColor.BLACK.ordinal());

        for(int i = 0; i < inventory.getSize(); i++) {
            if(inventory.getItem(i) == null) {
                inventory.setItem(i, bgItem);
            }
        }
    }

    /**
     * Creates the item's that will be used in the permissions GUI.
     */
    private static void createItems() {
        breakItem = new CFancyItem(Material.DIAMOND_PICKAXE)
                .setDisplayname("§eBreak Collector").setLore(Arrays.asList(required + Role.getByValue(Config.DESTROY_COLLECTOR_RANK).nicename,
                        "", cycle)).addItemFlag(ItemFlag.HIDE_ATTRIBUTES).build();

        placeItem = new CFancyItem(Material.LEATHER_HELMET)
                .setDisplayname("§ePlace Collector").setLore(Arrays.asList(required + Role.getByValue(Config.PLACE_COLLECTOR_RANK).nicename,
                        "", cycle)).build();

        useItem = new CFancyItem(Material.DIAMOND)
                .setDisplayname("§eUse Collector").setLore(Arrays.asList(required + Role.getByValue(Config.SELL_COLLECTOR_RANK).nicename,
                        "", cycle)).build();
    }

    @EventHandler
    public void onPermissionClick(InventoryClickEvent event) {
        if(event.getInventory().equals(inventory)) {
            event.setCancelled(true);

            int slot = event.getSlot();

            if(slot != 11 && slot != 13 && slot != 15) {
                return;
            }

            update(slot);
        }
    }

    private void update(int slot) {
        // Updating value...
        switch(slot) {
            case 11:
                int destroyRank = Config.DESTROY_COLLECTOR_RANK;
                Config.DESTROY_COLLECTOR_RANK = destroyRank == 4 ? 0 : destroyRank + 1;
                updateItem(inventory.getItem(11), Config.DESTROY_COLLECTOR_RANK);
                break;
            case 13:
                int placeRank = Config.PLACE_COLLECTOR_RANK;
                Config.PLACE_COLLECTOR_RANK = placeRank == 4 ? 0 : placeRank + 1;
                updateItem(inventory.getItem(13), Config.PLACE_COLLECTOR_RANK);
                break;
            case 15:
                int sellRank = Config.SELL_COLLECTOR_RANK;
                Config.SELL_COLLECTOR_RANK = sellRank == 4 ? 0 : sellRank + 1;
                updateItem(inventory.getItem(15), Config.SELL_COLLECTOR_RANK);
                break;
        }

        inventory.getViewers().forEach(viewer -> {
            if(viewer instanceof Player) {
                ((Player) viewer).updateInventory();
            }
        });
    }

    private void updateItem(ItemStack item, int rank) {
        ItemMeta meta = item.getItemMeta();

        meta.setLore(Arrays.asList(required + Role.getByValue(rank).nicename, "", cycle));

        item.setItemMeta(meta);
    }

    public static void save() {
        ChunkCollectorPlugin plugin = ChunkCollectorPlugin.getInstance();

        FileConfiguration permissions = plugin.getCollectorConfig().getPermissionsConfig();

        permissions.set("permissions.place-collector", Config.PLACE_COLLECTOR_RANK);

        permissions.set("permissions.destroy-collector", Config.DESTROY_COLLECTOR_RANK);

        permissions.set("permissions.sell-collector", Config.SELL_COLLECTOR_RANK);

        plugin.getCollectorConfig().savePermissionsConfig();
    }
}
