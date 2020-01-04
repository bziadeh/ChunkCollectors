package com.cloth.config;

import com.cloth.ChunkCollectorPlugin;

/**
 * Created by Brennan on 1/4/2020.
 */

public class Config {

    private ChunkCollectorPlugin plugin;

    public Config(ChunkCollectorPlugin instance) {
        this.plugin = instance;
    }

    public static String COLLECTOR_TITLE;
    public static String COLLECTOR_TYPE;
    public static String COLLECTOR_PLACE;
    public static String COLLECTOR_BREAK;
    public static String COLLECTOR_PLACE_FAILURE;
    public static String COLLECTOR_BREAK_FAILURE;
    public static String PREFIX_SUCCESS;
    public static String PREFIX_FAILURE;
    public static int INVENTORY_SIZE;

    /**
     * Sets up and loads the default config settings.
     *
     * @return the config.
     */
    public Config setup() {
        plugin.saveDefaultConfig();

        loadConfig(); // Asynchronously loads data from the configuration file.

        return this;
    }

    /**
     * Loads information from the configuration file.
     */
    public void loadConfig() {
        new Thread(() -> {
            COLLECTOR_TITLE = getString("title");
            COLLECTOR_TYPE = getString("collector.type");
            COLLECTOR_PLACE = getString("collector-place");
            COLLECTOR_PLACE_FAILURE = getString("collector-place-failure");
            COLLECTOR_BREAK = getString("collector-break");
            COLLECTOR_BREAK_FAILURE = getString("collector-break-failure");
            PREFIX_SUCCESS = getString("prefix-success");
            PREFIX_FAILURE = getString("prefix-failure");
            INVENTORY_SIZE = plugin.getConfig().getInt("size");
        }).start();
    }

    /**
     * Automatically replaces the config string's color codes.
     *
     * @param args the path to the string.
     * @return the string with translated color codes.
     */
    public String getString(String args) {
        return plugin.getConfig().getString(args)
                .replaceAll("&", "§");
    }
}
