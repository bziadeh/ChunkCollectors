package com.cloth.objects;

import com.cloth.util.LocationUtility;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.List;

/**
 * Created by Brennan on 1/7/2020.
 */
public class SafeBlock {

    private String location;

    public SafeBlock(Location location) {
        this.location = LocationUtility.convertLocationToString(location);
    }

    public Location getLocation() {
        return LocationUtility.convertStringToLocation(location);
    }
}
