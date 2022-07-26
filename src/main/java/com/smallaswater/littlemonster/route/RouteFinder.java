package com.smallaswater.littlemonster.route;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.generic.BaseFullChunk;
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

   @Setter
   protected boolean allowFuzzyResults = false;

   private int lastSetDestinationTick = 0;

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
      this.setDestination(destination, true);
   }

   public void setDestination(Vector3 destination, boolean enableOffset) {
      int tick = Server.getInstance().getTick();
      if (tick - this.lastSetDestinationTick < 15) {
         return;
      }
      this.lastSetDestinationTick = tick;
      this.destination = destination.clone();
      if (this.isFinished()) {
         this.finished = false;
      }
      if (this.isSearching()) {
         this.interrupt = true;
         //this.research();
      }
      RouteFinderThreadPool.executeRouteFinderThread(new RouteFinderSearchTask(this, enableOffset));
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
            return vec.getFloorX() == cur.getFloorX() && vec.getFloorZ() == cur.getFloorZ();
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

   public abstract boolean search(boolean enableOffset);

   public void research() {
      this.research(true);
   }

   public void research(boolean enableOffset) {
      this.resetNodes();
      this.search(enableOffset);
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

   public Block getBlockFast(Vector3 vector3) {
      return this.getBlockFast(vector3, true);
   }

   public Block getBlockFast(Vector3 vector3, boolean load) {
      return this.getBlockFast(vector3.getFloorX(), vector3.getFloorY(), vector3.getFloorZ(), load);
   }

   public Block getBlockFast(int x, int y, int z) {
      return this.getBlockFast(x, y, z, true);
   }

   public Block getBlockFast(int x, int y, int z, boolean load) {
      if (!"Nukkit".equals(Server.getInstance().getName())) {
         return this.level.getBlock(x, y, z, load);
      }

      int fullState;
      if (y >= 0 && y < 256) {
         int cx = x >> 4;
         int cz = z >> 4;
         BaseFullChunk chunk;
         if (load) {
            chunk = this.getLevel().getChunk(cx, cz);
         } else {
            chunk = this.getLevel().getChunkIfLoaded(cx, cz);
         }
         if (chunk != null) {
            fullState = chunk.getFullBlock(x & 0xF, y, z & 0xF);
         } else {
            fullState = 0;
         }
      } else {
         fullState = 0;
      }
      Block block = Block.fullList[fullState & 0xFFF].clone();
      block.x = x;
      block.y = y;
      block.z = z;
      block.level = this.getLevel();
      return block;
   }
}
