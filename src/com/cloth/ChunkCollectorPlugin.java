package com.cloth;

import com.cloth.collectors.ChunkCollector;
import com.cloth.collectors.CollectorHandler;
import com.cloth.commands.CollectorCommand;
import com.cloth.config.Config;
import com.cloth.config.SQL;
import com.cloth.inventory.InventoryCreator;
import com.cloth.inventory.InventoryHandler;
import com.cloth.inventory.Permissions;
import com.cloth.packets.PacketHandler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Brennan on 1/4/2020.
 */
public class ChunkCollectorPlugin extends JavaPlugin {

    private static ChunkCollectorPlugin instance;

    private CollectorHandler collectorHandler;

    private InventoryCreator inventoryCreator;

    private Config config;

    public static Economy economy = null;

    public void onEnable() {
        if(!setupEconomy()) {
            getLogger().severe("Cannot find Vault economy... disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
        }

        instance = this;

        // Loads content from config.yml.
        config = new Config(this).setup();

        // We create a latch so our threads are started at the correct time.
        CountDownLatch latch = new CountDownLatch(1);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Asynchronously create the table in our SQL database...
                SQL.createTableIfNotExists();
                // Creates our collector handler.
                collectorHandler = new CollectorHandler();
                // This inventory creator makes a default (and reusable) inventory for new collectors.
                inventoryCreator = new InventoryCreator(latch);
                // Handles the collector inventory (selling).
                new InventoryHandler();
                // Handles the chunk collector command.
                new CollectorCommand(instance);
                // Handles all packet related code.
                new PacketHandler(instance);
                // Setup our in-game permissions settings GUI.
                new Permissions();
                // Finally, load collectors from database.
                SQL.loadCollectors(latch);
            }
        }.runTaskLater(this, 30);
    }

    /**
     * Saves all collectors to the database when the server stops.
     */
    public void onDisable() {
        for(ChunkCollector chunkCollector : collectorHandler.getCollectorList()) {
            SQL.saveCollector(chunkCollector);
        }
        Permissions.save();
    }

    /**
     * Checks to ensure Vault economy is setup.
     * @return Whether or not the economy provider exists.
     */
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }

    /**
     * Registers the specified listener.
     *
     * @param listener the listener being registered.
     */
    public void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    /**
     * Unregisters the specified listener.
     *
     * @param listener the listener being unregistered.
     */
    public void unregisterListener(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    /**
     * Gets the chunk collector handler. Contains information on all collectors.
     *
     * @return the chunk collector handler.
     */
    public CollectorHandler getCollectorHandler() {
        return collectorHandler;
    }

    /**
     * Returns an instance of the main class.
     *
     * @return instance of the main class.
     */
    public static ChunkCollectorPlugin getInstance() {
        return instance;
    }

    /**
     * Gets the inventory creator.
     *
     * @return the inventory creator.
     */
    public InventoryCreator getInventoryCreator() {
        return inventoryCreator;
    }

    /**
     * Gets the collector config.
     *
     * @return the config.
     */
    public Config getCollectorConfig() {
        return config;
    }
}
