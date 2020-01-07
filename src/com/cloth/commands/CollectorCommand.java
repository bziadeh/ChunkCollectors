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

        if(!commandSender.hasPermission("chunkcollectors.give") && !commandSender.isOp()) {
            commandSender.sendMessage(Config.NO_PERMISSION);
            return false;
        }

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

        String type;

        if(!isCollectorType(type = args[2])) {
            commandSender.sendMessage(Config.COLLECTOR_INVALID);
            return false;
        }

        // The user didn't enter a valid amount.
        if(!args[3].matches("[0-9]+") || (args[3].matches("[0-9]+") && Integer.parseInt(args[3]) <= 0)) {
            commandSender.sendMessage(Config.INCORRECT_SYNTAX);
            sendBaseCommands(commandSender, label);
            return false;
        }

        int amount = Integer.parseInt(args[3]);

        // If the user's inventory is full, drop the item at their location.
        if(target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItemNaturally(target.getLocation(), ChunkCollector.getCollectorItem(type));
        } else {
            // use the type variable to change the displayname?
            target.getInventory().addItem(ChunkCollector.getCollectorItem(type));
        }

        // Send the sent and receive messages.
        String display = Config.COLLECTOR_ITEM_NAMES.get(args[2]).replaceAll("&", "§");
        sendCompleteMessage(commandSender, target, display, amount);
        return false;
    }

    /**
     * Sends the default information commands.
     *
     * @param commandSender who the messages are being sent to.
     * @param label the command (alias) the person used when entering the command.
     */
    private void sendBaseCommands(CommandSender commandSender, String label) {
        commandSender.sendMessage("§6/" + label + " give <player> <type> <amount>");
        commandSender.sendMessage("§6/" + label + " reload §7 - only reloads the messages.");
    }

    /**
     * Checks if the specified string is a collector type.
     *
     * @param args the string being checked.
     * @return whether or not the string is a collector type.
     */
    private boolean isCollectorType(String args) {
        for(String type : Config.COLLECTOR_TYPES) {
            if(type.equalsIgnoreCase(args))
                return true;
        }
        return false;
    }

    private void sendCompleteMessage(CommandSender sender, Player target, String type, int amount) {
        target.sendMessage(Config.COLLECTOR_RECEIVE
                .replaceAll("%amount%", String.valueOf(amount))
                .replaceAll("%type%", type)
                .replaceAll("%sender%", sender.getName()));

        if(!(sender instanceof Player) || !sender.equals(target)) {
            sender.sendMessage(Config.COLLECTOR_GIVE
                    .replaceAll("%name%", target.getName())
                    .replaceAll("%type%", type)
                    .replaceAll("%amount%", String.valueOf(amount)));
        }
    }
}
