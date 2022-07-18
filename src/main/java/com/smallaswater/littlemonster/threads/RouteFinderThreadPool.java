package com.smallaswater.littlemonster.threads;

import com.smallaswater.littlemonster.threads.runnables.RouteFinderSearchTask;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RouteFinderThreadPool {

   private static final ThreadPoolExecutor EXECUTOR;

   static {
      EXECUTOR = new ThreadPoolExecutor(
              1,
              Math.max(Runtime.getRuntime().availableProcessors(), 2),
              3,
              TimeUnit.SECONDS,
              new ArrayBlockingQueue<>(Math.max(Runtime.getRuntime().availableProcessors(), 2) * 4),
              task -> new Thread(task, "LittleMonster Pathfinding Tasks"),
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
