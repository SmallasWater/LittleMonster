package com.smallaswater.littlemonster.route;

import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import com.smallaswater.littlemonster.entity.baselib.BaseEntity;
import com.smallaswater.littlemonster.threads.RouteFinderThreadPool;
import com.smallaswater.littlemonster.threads.runnables.RouteFinderSearchTask;
import lombok.Getter;
import lombok.Setter;

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

   /**
    * 目的地偏离
    */
   @Getter
   @Setter
   protected double destinationDeviate;

   protected Level level;

   protected boolean interrupt = false;

   private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

   protected boolean reachable = true;

   RouteFinder(BaseEntity entity) {
      Objects.requireNonNull(entity, "RouteFinder: entity can not be null");
      this.entity = entity;
      this.start = this.entity;
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
      this.destination = destination.clone();
      if (this.isFinished()) {
         this.finished = false;
      }
      if (this.isSearching()) {
         this.interrupt = true;
         //this.research();
      }
      RouteFinderThreadPool.executeRouteFinderThread(new RouteFinderSearchTask(this));
   }

   public boolean isFinished() {
      return this.finished;
   }

   public boolean isSearching() {
      return this.searching;
   }

   public boolean needSearching() {
      return !this.isFinished() && !this.isSearching();
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
      try {
         this.lock.readLock().lock();
         if (this.hasCurrentNode()) {
            return this.nodes.get(this.current);
         }
      } finally {
         this.lock.readLock().unlock();
      }
      return null;
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
      try {
         lock.readLock().lock();
         if (this.hasNext() &&  this.getCurrentNode().getVector3()!=null) {
            Vector3 cur = this.getCurrentNode().getVector3();
            return vec.getX() == cur.getX() && vec.getZ() == cur.getZ();
         }
         return false;
      } finally {
         lock.readLock().unlock();
      }
   }

   public boolean hasArrivedNodeInaccurate(Vector3 vec) {
      try {
         lock.readLock().lock();
         if (this.hasNext() &&  this.getCurrentNode().getVector3()!=null) {
            Vector3 cur = this.getCurrentNode().getVector3();
            return cur.distance(vec) < 1;
         }
         return false;
      } finally {
         lock.readLock().unlock();
      }
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
      Vector3 vector3;
      try {
         this.lock.readLock().lock();
         if (!this.hasNext()) {
            return null;
         }
         vector3 = this.nodes.get(++this.current).getVector3();
      } finally {
         this.lock.readLock().unlock();
      }
      return vector3;
   }

   public boolean isInterrupted() {
      return this.interrupt;
   }

   public boolean interrupt() {
      return this.interrupt ^= true;
   }
}
