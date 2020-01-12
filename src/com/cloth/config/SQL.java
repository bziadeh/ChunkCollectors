package com.cloth.config;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.collectors.ChunkCollector;
import com.cloth.collectors.CollectorHandler;
import com.cloth.objects.ItemData;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import org.bukkit.Location;
import org.bukkit.Material;
import sun.security.krb5.Config;

import java.sql.*;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.sleep;

/**
 * Created by Brennan on 1/4/2020.
 */

public class SQL {

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

                String sql = "CREATE TABLE IF NOT EXISTS collectors (factionid TEXT, location TEXT PRIMARY KEY, contents TEXT, type TEXT);";

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
     * Saves the collector into the SQL database.
     *
     * @param collector the collector being saved.
     */
    public static void saveCollector(ChunkCollector collector) {
        new Thread(() -> {
            Connection connection = null;

            try {
                connection = getConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            PreparedStatement ps = null;

            String sql = "REPLACE INTO collectors VALUES(?, ?, ?, ?)";

            try {
                ps = connection.prepareStatement(sql);
                ps.setString(1, collector.getFaction().getId());
                ps.setString(2, collector.formatLocation());
                ps.setString(3, collector.formatContents());
                ps.setString(4, collector.getType());
                ps.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void removeCollector(ChunkCollector collector) {
        new Thread(() -> {
            Connection connection = null;

            try {
                connection = getConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            PreparedStatement ps = null;

            String sql = "DELETE FROM collectors WHERE location = ?";

            try {
                ps = connection.prepareStatement(sql);
                ps.setString(1, collector.formatLocation());
                ps.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void loadCollectors(CountDownLatch latch) {
        new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Connection connection = null;

            try {
                connection = getConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            Statement statement;

            String sql = "SELECT * FROM collectors";

            try {
                statement = connection.createStatement();

                ResultSet rs = statement.executeQuery(sql);

                CollectorHandler handler = ChunkCollectorPlugin.getInstance().getCollectorHandler();

                Converter converter = new Converter();

                while(rs.next()) {
                    Faction faction = Factions.getInstance().getFactionById(rs.getString("factionid"));

                    String location = rs.getString("location");

                    String contents = rs.getString("contents");

                    String type = rs.getString("type");

                    ChunkCollector chunkCollector = new ChunkCollector(faction, converter.convertStringToLocation(location), type);

                    chunkCollector.fill(contents);

                    handler.addCollector(chunkCollector);
                }

                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
