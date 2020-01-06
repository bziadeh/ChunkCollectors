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
    public static String COLLECTOR_MODE;
    public static String COLLECTOR_SELL;
    public static String INCORRECT_SYNTAX;
    public static String RELOAD_CONFIG;
    public static String PLAYER_NOT_FOUND;
    public static int INVENTORY_SIZE;

    /**
     * Sets up and loads the default config settings.
     *
     * @return the config.
     */
    public Config setup() {
        plugin.saveDefaultConfig();

        loadConfig(false); // Asynchronously loads data from the configuration file.

        return this;
    }

    /**
     * Loads information from the configuration file.
     */
    public void loadConfig(boolean reload) {
        new Thread(() -> {
            COLLECTOR_TITLE = getString("title");
            COLLECTOR_TYPE = getString("collector.type");
            COLLECTOR_PLACE = getString("collector-place");
            COLLECTOR_PLACE_FAILURE = getString("collector-place-failure");
            COLLECTOR_BREAK = getString("collector-break");
            COLLECTOR_BREAK_FAILURE = getString("collector-break-failure");
            COLLECTOR_MODE = getString("mode");
            COLLECTOR_SELL = getString("collector-sell");
            INCORRECT_SYNTAX = getString("incorrect-syntax");
            RELOAD_CONFIG = getString("reload-config");
            PLAYER_NOT_FOUND = getString("player-not-found");

            // We only allow messages to be reloaded, not inventory sizes. So we put this in here.
            if(!reload) {
                INVENTORY_SIZE = plugin.getConfig().getInt("size");
            }
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
