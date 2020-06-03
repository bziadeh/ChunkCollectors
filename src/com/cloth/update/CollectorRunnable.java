package com.cloth.update;

import com.cloth.ChunkCollectorPlugin;
import com.cloth.collectors.ChunkCollector;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class CollectorRunnable extends BukkitRunnable {

    private CollectorUpdateThread updateThread;
    private List<ChunkCollector> collectorList;

    public CollectorRunnable(CollectorUpdateThread updateThread) {
        this.updateThread = updateThread;
        this.collectorList = ChunkCollectorPlugin.getInstance()
                .getCollectorHandler().getCollectorList();
    }

    @Override
    public void run() {
        FutureTask<List<ChunkCollector>> future = cleanup();
        updateThread.addFuture(future);
    }

    public FutureTask<List<ChunkCollector>> cleanup() {
        FutureTask task = new FutureTask<>(() -> {
            final List<ChunkCollector> destroyed = new ArrayList<>();
            // Iterate backwards to prevent ConcurrentModificationException.
            for (int i = collectorList.size() - 1; i >= 0; i--) {
                final ChunkCollector temp = collectorList.get(i);
                // Collector was destroyed, cleanup.
                if (temp.getLocation().getBlock().getType() == Material.AIR) {
                    destroyed.add(temp);
                }
            }
            return destroyed;
        });
        task.run();
        return task;
    }
}
