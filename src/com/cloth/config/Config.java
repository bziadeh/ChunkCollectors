package com.cloth.config;

import com.cloth.ChunkCollectorPlugin;
import org.bukkit.configuration.file.FileConfiguration;

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
    public static String COLLECTOR_DENY;
    public static String INCORRECT_SYNTAX;
    public static String RELOAD_CONFIG;
    public static Set<String> COLLECTOR_TYPES;
    public static List<String> COLLECTOR_ITEM_LORE;
    public static Map<String, String> COLLECTOR_ITEM_NAMES;
    public static boolean COLLECTOR_ITEM_GLOWING;
    public static String PLAYER_NOT_FOUND;
    public static String NO_PERMISSION;
    public static String MUST_HAVE_FACTION;
    public static String CHUNK_OCCUPIED;
    public static int INVENTORY_SIZE;
    public static int PLACE_COLLECTOR_RANK;
    public static int DESTROY_COLLECTOR_RANK;
    public static int SELL_COLLECTOR_RANK;

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
            COLLECTOR_DENY = getString("collector-deny");
            INCORRECT_SYNTAX = getString("incorrect-syntax");
            RELOAD_CONFIG = getString("reload-config");
            PLAYER_NOT_FOUND = getString("player-not-found");
            NO_PERMISSION = getString("no-permission");
            MUST_HAVE_FACTION = getString("must-have-faction");
            CHUNK_OCCUPIED = getString("chunk-occupied");

            // We only allow messages to be reloaded, nothing else. So we put this stuff in here.
            if(!reload) {
                FileConfiguration config = plugin.getConfig();

                INVENTORY_SIZE = config.getInt("size");

                PLACE_COLLECTOR_RANK = config.getInt("permissions.place-collector");
                DESTROY_COLLECTOR_RANK = config.getInt("permissions.destroy-collector");
                SELL_COLLECTOR_RANK = config.getInt("permissions.sell-collector");

                COLLECTOR_TYPES = config.getConfigurationSection("collectors").getKeys(false);
                COLLECTOR_ITEM_NAMES = new HashMap<>();
                COLLECTOR_ITEM_GLOWING = config.getBoolean("collector.glowing");

                // Stores all of the custom item names.
                config.getConfigurationSection("collectors")
                        .getKeys(false).forEach(collector -> {
                    COLLECTOR_ITEM_NAMES.put(collector, getString("collectors." + collector + ".item-name"));
                });

                COLLECTOR_ITEM_LORE = (List<String>) config.getList("collector.lore");
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
