package com.cloth.update;

import com.cloth.collectors.ChunkCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Brennan on 6/2/2020.
 */
public class CollectorUpdateThread extends Thread {

    private List<Future<List<ChunkCollector>>> futureList = new ArrayList<>();

    public CollectorUpdateThread() {
        start();
    }

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!futureList.isEmpty()) {
                for(int i = futureList.size() - 1; i >= 0; i--) {
                    Future<List<ChunkCollector>> future = futureList.get(i);
                    while(!future.isDone()) {
                        // waiting for task to complete...
                    }
                    try {
                        List<ChunkCollector> collectorsToDestroy = future.get();
                        for(int j = collectorsToDestroy.size() - 1; j >= 0; j--) {
                            collectorsToDestroy.get(j).destroy(false);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void addFuture(Future future) {
        futureList.add(future);
    }
}
