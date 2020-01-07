package com.cloth.config;

import com.cloth.ChunkCollectorPlugin;

import java.util.*;

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
    public static String COLLECTOR_INVALID;
    public static String COLLECTOR_GIVE;
    public static String COLLECTOR_RECEIVE;
    public static String INCORRECT_SYNTAX;
    public static String RELOAD_CONFIG;
    public static Set<String> COLLECTOR_TYPES;
    public static List<String> COLLECTOR_ITEM_LORE;
    public static Map<String, String> COLLECTOR_ITEM_NAMES;
    public static String PLAYER_NOT_FOUND;
    public static String NO_PERMISSION;
    public static String MUST_HAVE_FACTION;
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
            COLLECTOR_INVALID = getString("collector-invalid");
            COLLECTOR_GIVE = getString("collector-give");
            COLLECTOR_RECEIVE = getString("collector-receive");
            INCORRECT_SYNTAX = getString("incorrect-syntax");
            RELOAD_CONFIG = getString("reload-config");
            PLAYER_NOT_FOUND = getString("player-not-found");
            NO_PERMISSION = getString("no-permission");
            MUST_HAVE_FACTION = getString("must-have-faction");


            // We only allow messages to be reloaded, nothing else. So we put this stuff in here.
            if(!reload) {
                INVENTORY_SIZE = plugin.getConfig().getInt("size");
                COLLECTOR_TYPES = plugin.getConfig().getConfigurationSection("collectors").getKeys(false);
                COLLECTOR_ITEM_NAMES = new HashMap<>();

                // Stores all of the custom item names.
                plugin.getConfig().getConfigurationSection("collectors")
                        .getKeys(false).forEach(collector -> {
                    COLLECTOR_ITEM_NAMES.put(collector, plugin.getConfig().getString("collectors." + collector + ".item-name"));
                });

                COLLECTOR_ITEM_LORE = (List<String>) plugin.getConfig().getList("collector.lore");
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
