package com.cloth.config;

import com.cloth.collectors.ChunkCollector;
import org.bukkit.Location;
import sun.security.krb5.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Brennan on 1/4/2020.
 */

public class SQLConnector {

    private static final String SQCONN = "jdbc:sqlite:collectors.sqlite";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(SQCONN);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void createTableIfNotExists() {
        // Create our table on a separate thread.
        new Thread(() -> {
            Connection connection = null;

            try {
                connection = getConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if(connection != null) {
                PreparedStatement preparedStatement = null;

                // The contents of this table will depend on the config. Does the user want the collector to store all drops, or only some?
                // Or, maybe create a table default and only use what the player specifies?
                String sql = "CREATE TABLE IF NOT EXISTS collectors (factionid INTEGER PRIMARY KEY NOT NULL, world TEXT, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL);";

                try {
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if(preparedStatement != null)
                            preparedStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Saves the collector into the SQLite database.
     *
     * @param collector the collector being saved.
     */
    public static void saveCollector(ChunkCollector collector) {
        Connection connection = null;

        try {
            connection = getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        PreparedStatement ps = null;

        String sql = "REPLACE INTO collectors VALUES(?, ?, ?, ?, ?)";

        try {

            Location location = collector.getLocation();
            ps = connection.prepareStatement(sql);

            // Store the id of the faction.
            ps.setString(1, collector.getFaction().getId());

            // Setting the location data.
            ps.setString(2, location.getWorld().getName());
            ps.setDouble(3, location.getX());
            ps.setDouble(4, location.getY());
            ps.setDouble(5, location.getZ());

            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if(ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeCollector(ChunkCollector collector) {

    }
}
