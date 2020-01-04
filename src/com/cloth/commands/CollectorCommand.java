package com.cloth.commands;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.collectors.ChunkCollector;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Brennan on 1/4/2020.
 */
public class CollectorCommand implements CommandExecutor {

    public CollectorCommand(ChunkCollectorPlugin plugin) {
        plugin.getCommand("collector").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        if(commandSender instanceof Player) {
            Player player = (Player) commandSender;

            player.getInventory().addItem(ChunkCollector.getCollectorItem());
        }

        return false;
    }
}
