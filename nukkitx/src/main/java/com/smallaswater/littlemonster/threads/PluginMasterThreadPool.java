package com.smallaswater.littlemonster.threads;

import com.smallaswater.littlemonster.threads.runnables.BasePluginThreadTask;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author SmallasWater
 * Create on 2021/6/29 8:17
 * Package com.smallaswater.littlemonster.threads
 */
public class PluginMasterThreadPool {

    public static final ExecutorService ASYNC_EXECUTOR;

    public static final ThreadPoolExecutor EXECUTOR;

    private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);

    static {
        ASYNC_EXECUTOR = Executors.newSingleThreadExecutor();
        EXECUTOR = new ThreadPoolExecutor(
                4,
                20,
                1L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                task -> new Thread(task, "LittleMonster Thread" + THREAD_COUNT.getAndIncrement()),
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
        ASYNC_EXECUTOR.shutdownNow();
        EXECUTOR.shutdownNow();
    }

    /**
     * 关停线程池并阻塞等待工作线程真正退出。
     * <p>{@link #shutDownNow()} 只发中断信号立即返回，调用方若紧接着访问被工作线程
     * 读写的共享状态（如 {@code PositionConfig.time} / {@code Config}），仍会与
     * 尚未退出的工作线程发生数据竞争（{@link cn.nukkit.utils.Config} 的内部 map
     * 非线程安全）。本方法在发中断后阻塞等待线程退出，保证调用返回时不再有工作
     * 线程访问共享状态。
     * <p>{@link cn.nukkit.utils.Config} 的内部 map 非线程安全；中断只在
     * {@code BasePluginThreadTask} 的 {@code Thread.sleep} 处生效，正在执行
     * {@code scheduler()} 的工作线程会跑完当前迭代后才退出，因此需要等待。
     *
     * @param timeoutSeconds 单池等待上限（秒）
     * @return 两个线程池是否都在超时前终止
     */
    public static boolean shutdownAndAwait(long timeoutSeconds) {
        shutDownNow();
        boolean asyncOk = false;
        boolean execOk = false;
        try {
            asyncOk = ASYNC_EXECUTOR.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            execOk = EXECUTOR.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return asyncOk && execOk;
    }

}
