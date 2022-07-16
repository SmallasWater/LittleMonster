package com.smallaswater.littlemonster.threads.runnables;

import com.smallaswater.littlemonster.route.RouteFinder;

public class RouteFinderSearchTask implements Runnable {

   private final RouteFinder route;
   private final boolean enableOffset;

   private int retryTime = 0;

   public RouteFinderSearchTask(RouteFinder route, boolean enableOffset) {
      this.route = route;
      this.enableOffset = enableOffset;
   }

   @Override
   public void run() {
      if (this.route != null) {
         while(this.retryTime < 50) {
            if (!this.route.isSearching()) {
               this.route.research(this.enableOffset);
               return;
            }
            this.retryTime += 10;
            try {
               Thread.sleep(100L);
            } catch (InterruptedException ignored) {
               return;
            }
         }
         //this.route.interrupt();
      }
   }
}
