package com.cloth;

import com.cloth.config.Config;

/**
 * Created by Brennan on 5/3/2020.
 */
public class CollectorBackups extends Thread {

    // How often are the collectors saved to JSON.
    private static final int BACKUP_INTERVAL = 1000 * 60 * Config.COLLECTOR_BACKUP_INTERVAL;

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(BACKUP_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ChunkCollectorPlugin.getInstance().getCollectorHandler().backup();
        }
    }
}
