package com.smallaswater.littlemonster.threads;

import com.smallaswater.littlemonster.threads.runnables.RouteFinderSearchTask;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RouteFinderThreadPool {

   private static final ThreadPoolExecutor EXECUTOR;

   private static final AtomicInteger THREAD_COUNT = new AtomicInteger(0);

   static {
      EXECUTOR = new ThreadPoolExecutor(
              1,
              Math.max(Runtime.getRuntime().availableProcessors(), 2),
              5,
              TimeUnit.SECONDS,
              new ArrayBlockingQueue<>(Math.max(Runtime.getRuntime().availableProcessors(), 2) * 4),
              task -> {
                 Thread thread = new Thread(task, "LittleMonster Pathfinding Thread" + THREAD_COUNT.getAndIncrement());
                 thread.setPriority(Math.max(thread.getPriority() - 2, Thread.MIN_PRIORITY));
                 return thread;
              },
              new ThreadPoolExecutor.DiscardPolicy()
      );
   }

   private RouteFinderThreadPool() {
      throw new RuntimeException();
   }

   public static void executeRouteFinderThread(RouteFinderSearchTask t) {
      if (!EXECUTOR.isShutdown() && !EXECUTOR.isTerminating()) {
         EXECUTOR.execute(t);
      }
   }

   public static void shutDownNow() {
      EXECUTOR.shutdownNow();
   }

}
