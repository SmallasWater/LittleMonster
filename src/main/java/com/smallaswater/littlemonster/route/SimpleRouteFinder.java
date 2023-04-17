package com.smallaswater.littlemonster.route;

import com.smallaswater.littlemonster.entity.baselib.BaseEntity;


public class SimpleRouteFinder extends RouteFinder {

   public SimpleRouteFinder(BaseEntity entity) {
      super(entity);
   }

   @Override
   public boolean search() {
      this.resetNodes();
      this.addNode(new Node(this.destination));
      return true;
   }
}
