package com.cloth.objects;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import java.util.List;

/**
 * Created by Brennan on 1/7/2020.
 */
public class SafeBlock {

    private Location location;

    public SafeBlock(Location location) {
        this.location = location;
    }

    @EventHandler
    public void onBlockFall(BlockFromToEvent event) {
        if(event.getBlock().getLocation().equals(location)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if(event.getBlock().getLocation().equals(location)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if(containsChunkCollector(event.getBlocks()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if(containsChunkCollector(event.getBlocks()))
            event.setCancelled(true);
    }

    private boolean containsChunkCollector(List<Block> blockList) {
        for(Block b : blockList)
            if(b.getLocation().equals(location))
                return true;
        return false;
    }
}
