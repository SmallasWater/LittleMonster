package com.smallaswater.littlemonster.threads;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.smallaswater.littlemonster.threads.runnables.RouteFinderSearchTask;

public class RouteFinderThreadPool {

   private static final ThreadPoolExecutor EXECUTOR;

   static {
      EXECUTOR = new ThreadPoolExecutor(
              1,
              Runtime.getRuntime().availableProcessors() + 1,
              1L,
              TimeUnit.SECONDS,
              new LinkedBlockingQueue<>(),
              new ThreadPoolExecutor.AbortPolicy()
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
