package com.cloth.packets;

import com.cloth.ChunkCollectorPlugin;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Brennan on 1/8/2020.
 */
public class PacketHandler {

    private File file;

    private FileConfiguration config;

    private String message;

    private ChunkCollectorPlugin plugin;

    public static Set<String> playersToRemove;

    public PacketHandler(ChunkCollectorPlugin plugin) {
        this.plugin = plugin;

        playersToRemove = new HashSet<>();

        setup();
    }

    private void setup() {
        new Thread() {
            @Override
            public void run() {
                file = new File(plugin.getServer().getPluginManager().getPlugin("Factions").getDataFolder().getPath() + "/lang.yml");

                config = new YamlConfiguration();

                try {
                    config.load(file);
                } catch (IOException | InvalidConfigurationException e) {
                    e.printStackTrace();
                }

                message = config.getString("PLAYER.USE.TERRITORY");

                ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.CHAT) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        super.onPacketReceiving(event);
                    }

                    @Override
                    public void onPacketSending(PacketEvent event) {
                        WrapperPlayServerChat wrapper = new WrapperPlayServerChat(event.getPacket());

                        JSONObject jsonObject = new JSONObject(wrapper.getMessage().getJson());

                        if(jsonObject.has("extra")) {
                            try {
                                JSONArray jsonArray = (JSONArray) jsonObject.get("extra");

                                double similar = getSimilarity(jsonArray.get(0).toString(), message);

                                if(similar > 0.8) {

                                    event.setCancelled(true);

                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {

                                            if(PacketHandler.playersToRemove.contains(event.getPlayer().getName())) {

                                                playersToRemove.remove(event.getPlayer().getName());

                                            } else {
                                                jsonArray.put(0, jsonArray.get(0) + "§k");

                                                IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(jsonObject.toString());

                                                Player player = event.getPlayer();

                                                PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

                                                connection.sendPacket(new PacketPlayOutChat(component));
                                            }
                                        }
                                    }.runTaskLater(ChunkCollectorPlugin.getInstance(), 2);
                                }
                            } catch (JSONException exception) {
                                // The JSONObject text was invalid...
                            }
                        }
                    }
                });
            }
        }.start();
    }

    private double getSimilarity(String a, String b) {

        if(a.endsWith("§k") || b.endsWith("§k")) {
            return 0;
        }

        String[] asplit = a.split(" ");

        String[] bsplit = b.split(" ");

        if(asplit.length != bsplit.length) {
            return 0;
        }

        double same = 0;

        // they are the same length, so this works fine.
        for(int i = 0; i < asplit.length; i++) {
            if(asplit[i].equalsIgnoreCase(bsplit[i])) {
                same++;
            }
        }

        return same / asplit.length;
    }
}