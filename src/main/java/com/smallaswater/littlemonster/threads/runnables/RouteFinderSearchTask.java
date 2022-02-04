package com.smallaswater.littlemonster.threads.runnables;


import com.smallaswater.littlemonster.route.RouteFinder;

public class RouteFinderSearchTask implements Runnable {

   private RouteFinder route;

   private int retryTime = 0;

   public RouteFinderSearchTask(RouteFinder route) {
      this.route = route;
   }

   @Override
   public void run() {
      if (this.route != null) {
         while(this.retryTime < 50) {
            if (!this.route.isSearching()) {
               this.route.research();
               return;
            }
            this.retryTime += 10;
            try {
               Thread.sleep(100L);
            } catch (InterruptedException ignored) {
            }
         }
         //this.route.interrupt();
      }
   }
}
