package com.cloth.config;

import com.cloth.ChunkCollectorPlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Brennan on 1/4/2020.
 */
public class Config {

    private ChunkCollectorPlugin plugin;

    public Config(ChunkCollectorPlugin instance) {
        this.plugin = instance;
    }

    private File file;
    private FileConfiguration permissionsConfig;

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
    public static String COLLECTOR_EXTRACT;
    public static String INCORRECT_SYNTAX;
    public static String RELOAD_CONFIG;
    public static Set<String> COLLECTOR_TYPES;
    public static List<String> COLLECTOR_ITEM_LORE;
    public static Map<String, String> COLLECTOR_ITEM_NAMES;
    public static boolean COLLECTOR_ITEM_GLOWING;
    public static boolean COLLECTOR_HOLOGRAMS_ENABLED;
    public static String PLAYER_NOT_FOUND;
    public static String NO_PERMISSION;
    public static String MUST_HAVE_FACTION;
    public static String CHUNK_OCCUPIED;
    public static int INVENTORY_SIZE;
    public static int PLACE_COLLECTOR_RANK;
    public static int DESTROY_COLLECTOR_RANK;
    public static int SELL_COLLECTOR_RANK;
    public static boolean TNT_BANK_ENABLED;
    public static boolean DISABLE_NATURAL_FARMS;
    public static int COLLECTOR_BACKUP_INTERVAL;
    public static boolean CLEANUP_RUNNABLE;

    /**
     * Sets up and loads the default config settings.
     *
     * @return the config.
     */
    public Config setup() {
        plugin.saveDefaultConfig();

        setPermissionDefaults();

        loadConfig(false); // Asynchronously loads data from the configuration file.

        return this;
    }

    /**
     * Loads information from the configuration file.
     */
    public void loadConfig(boolean reload) {
        new Thread(() -> {
            FileConfiguration config = plugin.getConfig();

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
            COLLECTOR_EXTRACT = getString("collector-extract");
            INCORRECT_SYNTAX = getString("incorrect-syntax");
            RELOAD_CONFIG = getString("reload-config");
            PLAYER_NOT_FOUND = getString("player-not-found");
            NO_PERMISSION = getString("no-permission");
            MUST_HAVE_FACTION = getString("must-have-faction");
            CHUNK_OCCUPIED = getString("chunk-occupied");
            TNT_BANK_ENABLED = config.getBoolean("tnt-bank");
            DISABLE_NATURAL_FARMS = config.getBoolean("disable-natural-farms");
            COLLECTOR_BACKUP_INTERVAL = config.getInt("collector-backup-interval");
            CLEANUP_RUNNABLE = config.getBoolean("cleanup-runnable");

            // Ensure the interval is AT LEAST once per minute. No quicker than that.
            if(COLLECTOR_BACKUP_INTERVAL < 1) COLLECTOR_BACKUP_INTERVAL = 1;

            // We only allow messages to be reloaded, nothing else. So we put this stuff in here.
            if(!reload) {
                INVENTORY_SIZE = config.getInt("size");

                COLLECTOR_TYPES = config.getConfigurationSection("collectors").getKeys(false);
                COLLECTOR_ITEM_NAMES = new HashMap<>();
                COLLECTOR_ITEM_GLOWING = config.getBoolean("collector.glowing");
                COLLECTOR_HOLOGRAMS_ENABLED = config.getBoolean("holograms");

                // Stores all of the custom item names.
                config.getConfigurationSection("collectors")
                        .getKeys(false).forEach(collector -> {
                    COLLECTOR_ITEM_NAMES.put(collector.toLowerCase(), getString("collectors." + collector + ".item-name"));
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

    public FileConfiguration getPermissionsConfig() {
        return permissionsConfig;
    }

    public void savePermissionsConfig() {
        try {
            permissionsConfig.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setPermissionDefaults() {
        new Thread(() -> {
            file = new File(plugin.getDataFolder(), "permissions.yml");

            if(!file.exists()) {
                file.getParentFile().mkdirs();
                plugin.saveResource("permissions.yml", false);
            }

            permissionsConfig = new YamlConfiguration();

            try {
                permissionsConfig.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }

            if(!permissionsConfig.contains("permissions")) {

                permissionsConfig.set("permissions.place-collector", 2);

                permissionsConfig.set("permissions.destroy-collector", 2);

                permissionsConfig.set("permissions.sell-collector", 2);

                savePermissionsConfig();
            }

            PLACE_COLLECTOR_RANK = permissionsConfig.getInt("permissions.place-collector");

            DESTROY_COLLECTOR_RANK = permissionsConfig.getInt("permissions.destroy-collector");

            SELL_COLLECTOR_RANK = permissionsConfig.getInt("permissions.sell-collector");

        }).start();
    }
}
