package com.cloth.commands;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.collectors.ChunkCollector;
import com.cloth.config.Config;
import org.bukkit.Bukkit;
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
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {

        if(args.length == 0) {
            sendBaseCommands(commandSender, label);
            return false;
        }

        if(args.length == 1) {

            if(!args[0].equalsIgnoreCase("reload")) {
                commandSender.sendMessage(Config.INCORRECT_SYNTAX);
                sendBaseCommands(commandSender, label);
                return false;
            }

            ChunkCollectorPlugin.getInstance().getCollectorConfig().loadConfig(true);
            commandSender.sendMessage(Config.RELOAD_CONFIG);
            return true;
        }

        if(args.length != 4 || (args.length == 4 && !args[0].equalsIgnoreCase("give"))) {
            commandSender.sendMessage(Config.INCORRECT_SYNTAX);
            sendBaseCommands(commandSender, label);
            return false;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if(target == null) {
            commandSender.sendMessage(Config.PLAYER_NOT_FOUND);
            return false;
        }


        return false;
    }

    private void sendBaseCommands(CommandSender commandSender, String label) {
        commandSender.sendMessage("§6/" + label + " give <player> <type> <amount>");
        commandSender.sendMessage("§6/" + label + " reload §7 - only reloads the messages.");
    }
}
