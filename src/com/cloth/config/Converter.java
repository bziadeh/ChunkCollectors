package com.cloth.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Created by Brennan on 1/9/2020.
 */
public class Converter {

    public Location convertStringToLocation(String location) {
        String[] loc = location.split(",");

        World world = Bukkit.getWorld(loc[0]);
        double x = Double.parseDouble(loc[1]);
        double y = Double.parseDouble(loc[2]);
        double z = Double.parseDouble(loc[3]);

        return new Location(world, x, y, z);
    }
}
