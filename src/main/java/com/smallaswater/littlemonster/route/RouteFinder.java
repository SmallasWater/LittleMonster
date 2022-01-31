package com.smallaswater.littlemonster.route;

import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import com.smallaswater.littlemonster.entity.baselib.BaseEntity;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class RouteFinder {

   protected ArrayList<Node> nodes = new ArrayList<>();

   protected boolean finished = false;

   protected boolean searching = false;

   protected int current = 0;

   public BaseEntity entity;

   protected Vector3 start;

   protected Vector3 destination;

   protected Level level;

   protected boolean interrupt = false;

   private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

   protected boolean reachable = true;

   RouteFinder(BaseEntity entity) {
      Objects.requireNonNull(entity, "RouteFinder: entity can not be null");
      this.entity = entity;
      this.start = this.entity.clone();
      this.level = entity.getLevel();
   }

   public BaseEntity getEntity() {
      return this.entity;
   }

   public Vector3 getStart() {
      return this.start;
   }

   public void setStart(Vector3 start) {
      if (!this.isSearching()) {
         this.start = start;
      }

   }

   public Vector3 getDestination() {
      return this.destination;
   }

   public void setDestination(Vector3 destination) {
      this.destination = destination;
      if (this.isSearching()) {
         this.interrupt = true;
         this.research();
      }

   }

   public boolean isFinished() {
      return this.finished;
   }

   public boolean isSearching() {
      return this.searching;
   }

   public void addNode(Node node) {
      try {
         this.lock.writeLock().lock();
         this.nodes.add(node);
      } finally {
         this.lock.writeLock().unlock();
      }

   }

   public void addNode(ArrayList<Node> node) {
      try {
         this.lock.writeLock().lock();
         this.nodes.addAll(node);
      } finally {
         this.lock.writeLock().unlock();
      }

   }

   public boolean isReachable() {
      return this.reachable;
   }

   public Node getCurrentNode() {
     Node var1;
      try {
         this.lock.readLock().lock();
         if (this.hasCurrentNode()) {
            var1 = this.nodes.get(this.current);
            return var1;
         }

         var1 = null;
      } finally {
         this.lock.readLock().unlock();
      }

      return var1;
   }

   public boolean hasCurrentNode() {
      return this.current < this.nodes.size();
   }

   public Level getLevel() {
      return this.level;
   }

   public void setLevel(Level level) {
      this.level = level;
   }

   public int getCurrent() {
      return this.current;
   }

   public boolean hasArrivedNode(Vector3 vec) {
      boolean var3;
      try {
         this.lock.readLock().lock();
         if (!this.hasNext() || this.getCurrentNode().getVector3() == null) {
            return false;
         }

         Vector3 cur = this.getCurrentNode().getVector3();
         var3 = vec.getX() == cur.getX() && vec.getZ() == cur.getZ();
      } finally {
         this.lock.readLock().unlock();
      }

      return var3;
   }

   public void resetNodes() {
      try {
         this.lock.writeLock().lock();
         this.nodes.clear();
         this.current = 0;
         this.interrupt = false;
         this.destination = null;
      } finally {
         this.lock.writeLock().unlock();
      }
   }

   public abstract boolean search();

   public void research() {
      this.resetNodes();
      this.search();
   }

   public boolean hasNext() {
      return this.current + 1 < this.nodes.size() && this.nodes.get(this.current + 1) != null;
   }

   public Vector3 next() {
      Vector3 var1;
      try {
         this.lock.readLock().lock();
         if (!this.hasNext()) {
            var1 = null;
            return var1;
         }

         var1 = this.nodes.get(++this.current).getVector3();
      } finally {
         this.lock.readLock().unlock();
      }

      return var1;
   }

   public boolean isInterrupted() {
      return this.interrupt;
   }

   public boolean interrupt() {
      return this.interrupt ^= true;
   }
}
