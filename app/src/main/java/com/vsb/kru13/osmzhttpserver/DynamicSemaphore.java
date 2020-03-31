package com.vsb.kru13.osmzhttpserver;

import android.util.Log;
import java.util.concurrent.Semaphore;

public class DynamicSemaphore extends Semaphore {
    private int minimumThreadsCount;
    private int maximumThreadsCount;

    public DynamicSemaphore() {
        super(10);
        this.minimumThreadsCount = 0;
        this.maximumThreadsCount = 10;
    }

    public DynamicSemaphore(int maximumThreads) {
        super(maximumThreads);
        this.minimumThreadsCount = 0;
        this.maximumThreadsCount = maximumThreads;
    }

    public DynamicSemaphore(int minimumThreads, int maximumThreads) {
        super(maximumThreads);
        this.minimumThreadsCount = minimumThreads;
        this.maximumThreadsCount = maximumThreads;
    }

    public synchronized boolean extendThreadsPool(int newThreadsCount) {
        int threadsToAdd = newThreadsCount - this.maximumThreadsCount;

        if (threadsToAdd == 0) {
            Log.d("SEMAPHORE", "No new threads to add, aborting operation...");
            return false;
        }
        else if (threadsToAdd > 0) {
            Log.d("SEMAPHORE", "Extending threads pool by " + threadsToAdd + " threads");
            this.release(threadsToAdd);
        }
        else if (this.minimumThreadsCount > (this.maximumThreadsCount + threadsToAdd)) {
            Log.d("SEMAPHORE", "ERROR: Can't shrink thread pool bellow " + this.minimumThreadsCount + " thread");
            return false;
        }
        else if (this.availablePermits() >= Math.abs(threadsToAdd)) {
            Log.d("SEMAPHORE", "Shrinking threads pool by " + Math.abs(threadsToAdd) + " threads");
            this.reducePermits(Math.abs(threadsToAdd));
        }
        else {
            Log.d("SEMAPHORE", "ERROR: Can't shrink thread pool, not enough free threads");
            Log.d("SEMAPHORE", "Expected (at least): " + Math.abs(threadsToAdd) + " free, got: " + this.availablePermits());
            return false;
        }

        this.maximumThreadsCount = newThreadsCount;
        Log.d("SEMAPHORE", "Current threads count: " + this.maximumThreadsCount);

        return true;
    }

    public int getThreadsCount() {
        return this.maximumThreadsCount;
    }
}
