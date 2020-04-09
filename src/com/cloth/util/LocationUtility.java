package com.cloth.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Created by Brennan on 3/1/2020.
 */
public class LocationUtility {
    public static Location convertStringToLocation(String locationString) {
        String[] data = locationString.split(",");
        String world = data[0];
        double x = Double.parseDouble(data[1]);
        double y = Double.parseDouble(data[2]);
        double z = Double.parseDouble(data[3]);
        return new Location(Bukkit.getWorld(world), x, y, z);
    }

    public static String convertLocationToString(Location location) {
        return String.format("%s,%.2f,%.2f,%.2f", location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ());
    }
}
