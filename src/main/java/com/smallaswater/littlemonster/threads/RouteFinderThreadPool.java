package com.smallaswater.littlemonster.threads;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

import com.smallaswater.littlemonster.threads.runnables.RouteFinderSearchTask;

public class RouteFinderThreadPool {

   private static final ThreadPoolExecutor executor;

   static {
      executor = new ThreadPoolExecutor(
              1,
              Runtime.getRuntime().availableProcessors() + 1,
              1L,
              TimeUnit.SECONDS,
              new LinkedBlockingQueue<>(),
              new AbortPolicy()
      );
   }

   private RouteFinderThreadPool() {
      throw new RuntimeException();
   }

   public static void executeRouteFinderThread(RouteFinderSearchTask t) {
      if (!executor.isShutdown() && !executor.isTerminating()) {
         executor.execute(t);
      }
   }

   public static void shutDownNow() {
      executor.shutdownNow();
   }

}
