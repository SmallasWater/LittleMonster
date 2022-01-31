package com.smallaswater.littlemonster.threads;

import com.smallaswater.littlemonster.threads.runnables.BasePluginThreadTask;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author SmallasWater
 * Create on 2021/6/29 8:17
 * Package com.smallaswater.littlemonster.threads
 */
public class PluginMasterThreadPool {

    public static final ThreadPoolExecutor EXECUTOR;

    static {
        EXECUTOR = new ThreadPoolExecutor(
                3,
                20,
                1L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    private PluginMasterThreadPool() {
        throw new RuntimeException();
    }

    public static void executeThread(BasePluginThreadTask t) {
        if (!EXECUTOR.isShutdown() && !EXECUTOR.isTerminating()) {
            EXECUTOR.execute(t);
        }
    }

    public static void shutDownNow() {
        EXECUTOR.shutdownNow();
    }

}
