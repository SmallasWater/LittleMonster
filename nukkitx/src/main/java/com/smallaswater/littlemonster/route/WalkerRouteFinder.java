package com.smallaswater.littlemonster.route;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockWater;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Position;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.entity.baselib.Area;
import com.smallaswater.littlemonster.entity.baselib.BaseEntity;
import com.smallaswater.littlemonster.utils.Utils;

import java.util.*;


public class WalkerRouteFinder extends SimpleRouteFinder {

   private static final int DIRECT_MOVE_COST = 10;

   private static final int OBLIQUE_MOVE_COST = 14;

   private final PriorityQueue<Node> openList = new PriorityQueue<>();

   private final ArrayList<Node> closeList = new ArrayList<>();

   private int searchLimit = 100; //搜索步数限制

   public WalkerRouteFinder(BaseEntity entity) {
      super(entity);
      this.level = entity.getLevel();
   }

   public WalkerRouteFinder(BaseEntity entity, Vector3 start) {
      super(entity);
      this.level = entity.getLevel();
      this.start = start;
   }

   public WalkerRouteFinder(BaseEntity entity, Vector3 start, Vector3 destination) {
      super(entity);
      this.level = entity.getLevel();
      this.start = start;
      this.originalDestination = destination.clone();
   }

   private int calHeuristic(Vector3 pos1, Vector3 pos2) {
      return 10 * (Math.abs(pos1.getFloorX() - pos2.getFloorX()) + Math.abs(pos1.getFloorZ() - pos2.getFloorZ())) + 11 * Math.abs(pos1.getFloorY() - pos2.getFloorY());
   }

   @Override
   public boolean search() {
      if (LittleMonsterMainClass.debug) {
         LittleMonsterMainClass.getInstance().getLogger().info("[debug] 实体" + this.entity.getName() + " 寻路开始");
      }
      if (this.entity.getTargetVector() == null && this.originalDestination == null) {
         this.searching = false;
         this.finished = true;
         if (LittleMonsterMainClass.debug) {
            LittleMonsterMainClass.getInstance().getLogger().info("[debug] 实体" + this.entity.getName() + " 寻路失败 没有目标");
         }
         return false;
      }

      this.finished = false;
      this.searching = true;
      if (this.start == null) {
         this.start = this.entity;
      }

      try {
         this.destinationLock.lock();
         if (this.originalDestination == null && this.entity.getTargetVector() != null) {
            this.originalDestination = this.entity.getTargetVector().clone();
         }

         if (this.originalDestination == null) {
            this.searching = false;
            this.finished = true;
            if (LittleMonsterMainClass.debug) {
               LittleMonsterMainClass.getInstance().getLogger().info("[debug] 实体" + this.entity.getName() + " 寻路失败 没有目标");
            }
            return false;
         }

         //找一个可以站立的目标点
         Position safeSpawn = this.level.getSafeSpawn(this.originalDestination);
         if (safeSpawn.distance(this.originalDestination) < 10) {
            this.destination = safeSpawn;
         } else {
            this.destination = this.originalDestination;
         }
      } finally {
         this.destinationLock.unlock();
      }

      try {
         if (this.enableOffset && this.destinationDeviate > 0) {
            double x = Utils.rand(this.destinationDeviate * 0.8, this.destinationDeviate);
            double z = Utils.rand(this.destinationDeviate * 0.8, this.destinationDeviate);
            Vector3 vector3 = this.destination.add(Utils.rand() ? x : -x, 5, Utils.rand() ? z : -z);
            for (int i=0; i<10; i++) {
               if (this.isPassable(vector3)) {
               /*if (this.level.getBlock(vector3.up()).canPassThrough() &&
                       this.level.getBlock(vector3).canPassThrough() &&
                       !this.level.getBlock(vector3.down()).canPassThrough()) {*/
                  this.destination = vector3;
                  break;
               }
               vector3.y--;
            }
         }
      } catch (Exception e) {
         LittleMonsterMainClass.getInstance().getLogger().error("设置终点偏移错误", e);
      }

      this.resetTemporary();
      Node presentNode = new Node(this.start);
      this.closeList.add(new Node(this.start));

      try {
         while (!this.isPositionOverlap(presentNode.getVector3(), this.destination)) {
            if (this.isInterrupted()) {
               this.searchLimit = 0;
               this.searching = false;
               this.finished = true;
               if (LittleMonsterMainClass.debug) {
                  LittleMonsterMainClass.getInstance().getLogger().info("[debug] 实体" + this.entity.getName() + " 寻路失败 被中断");
               }
               return false;
            }

            this.putNeighborNodeIntoOpen(presentNode);
            if (this.openList.peek() == null || this.searchLimit-- <= 0) {
               this.searching = false;
               this.finished = true;
               this.reachable = false;
               this.addNode(new Node(this.destination));
               if (LittleMonsterMainClass.debug) {
                  LittleMonsterMainClass.getInstance().getLogger().info("[debug] 实体" + this.entity.getName() + " 寻路失败 找不到路径");
               }
               if (this.allowFuzzyResults) {
                  //TODO 检查这个返回最近位置路径的方法
                  LinkedList<Node> list = new LinkedList<>(this.closeList);
                  list.sort(Comparator.comparingInt(Node::getF));
                  Node node = list.getFirst();
                  list.clear();
                  list.add(node);
                  while ((node = node.getParent()) != null) {
                     list.addFirst(node);
                  }
                  this.closeList.clear();
                  this.closeList.addAll(list);
                  if (LittleMonsterMainClass.debug) {
                     LittleMonsterMainClass.getInstance().getLogger().info("[debug] 实体" + this.entity.getName() + " 寻路失败 返回最靠近的位置");
                  }
               }else {
                  return false;
               }
            }

            this.closeList.add(presentNode = this.openList.poll());
         }

         if (!presentNode.getVector3().equals(this.destination)) {
            this.closeList.add(new Node(this.destination, presentNode, 0, 0));
         }

         ArrayList<Node> findingPath = this.getPathRoute();
         findingPath = this.FloydSmooth(findingPath);
         this.resetNodes();
         this.addNode(findingPath);
         this.finished = true;
         this.searching = false;
         if (LittleMonsterMainClass.debug) {
            LittleMonsterMainClass.getInstance().getLogger().info("[debug] 实体" + this.entity.getName() + " 寻路完成 路径数量" + this.getPathRoute().size());
            this.show();
         }
         return true;
      }catch (Exception e) {
         if (!(this.entity == null || this.entity.isClosed() || this.entity.getFollowTarget() == null ||
                 this.entity.getFollowTarget().isClosed())) {
            LittleMonsterMainClass.getInstance().getLogger().error("寻路错误", e);
         }
         this.searching = false;
         this.finished = true;
         this.reachable = false;
         if (this.destination != null) {
            this.addNode(new Node(this.destination));
         }
         if (LittleMonsterMainClass.debug) {
            LittleMonsterMainClass.getInstance().getLogger().info("[debug] 实体" + this.entity.getName() + " 寻路失败", e);
         }
         return false;
      }
   }

   public void show() {
      if (!LittleMonsterMainClass.debug) {
         return;
      }
      for (Node node : this.getPathRoute()) {
         this.level.addParticleEffect(node, ParticleEffect.REDSTONE_TORCH_DUST);
      }
   }

   private Block getHighestUnder(Vector3 vector3, int limit) {
      if (limit > 0) {
         for (int y = vector3.getFloorY(); y >= vector3.getFloorY() - limit; y--) {
            Block block = this.getBlockFast(vector3.getFloorX(), y, vector3.getFloorZ(), false);
            if (this.isWalkable(block) && this.getBlockFast(block.add(0, 1, 0), false).canPassThrough()) {
               return block;
            }
         }
         return null;
      }
      for (int y = vector3.getFloorY(); y >= 0; y--) {
         Block block = this.getBlockFast(vector3.getFloorX(), y, vector3.getFloorZ(), false);
         if (this.isWalkable(block) && this.getBlockFast(block.add(0, 1, 0), false).canPassThrough()) {
            return block;
         }
      }
      return null;
   }

   private boolean canWalkOn(Block block) {
      return block.getId() != BlockID.LAVA &&
              block.getId() != BlockID.STILL_LAVA &&
              block.getId() != BlockID.CACTUS &&
              block.getId() != BlockID.FIRE;
   }

   private boolean isWalkable(Vector3 vector3) {
      Block block = this.getBlockFast(vector3, false);
      return (!block.canPassThrough() && this.canWalkOn(block)) ||
              (block instanceof BlockWater); //允许在水上走
   }

   private boolean isPassable(Vector3 vector3) {
      double radius = this.entity.getWidth() * this.entity.getScale() / 2.0F;
      float height = this.entity.getHeight() * this.entity.getScale();
      AxisAlignedBB bb = new Area(vector3.getX() - radius, vector3.getY(), vector3.getZ() - radius, vector3.getX() + radius, vector3.getY() + (double)height, vector3.getZ() + radius);
      Block[] collisionBlocks = this.level.getCollisionBlocks(bb);
      for (Block block : collisionBlocks) {
         if (!block.canPassThrough()) {
            return false;
         }
      }
      Block block = this.getBlockFast(vector3.add(0.0D, -1.0D, 0.0D), false);
      return !block.canPassThrough() || (block instanceof BlockWater);
   }

   private boolean isPassable(Vector3 now, Vector3 target) {
      //跳跃检查
      if (target.getFloorY() > now.getFloorY() && !this.getBlockFast(now.add(0, 2, 0)).canPassThrough()) {
         return false;
      }

      return this.isPassable(target);
   }

   private int getWalkableHorizontalOffset(Vector3 vector3) {
      Block block = getHighestUnder(vector3, 4);
      if (block != null) {
         return (block.getFloorY() - vector3.getFloorY()) + 1;
      }
      return -256;
   }

   public int getSearchLimit() {
      return this.searchLimit;
   }

   public void setSearchLimit(int limit) {
      this.searchLimit = limit;
   }

   private void putNeighborNodeIntoOpen(Node node) {
      boolean N, E, S, W;

      Vector3 vector3 = new Vector3(node.getVector3().getFloorX() + 0.5, node.getVector3().getY(), node.getVector3().getFloorZ() + 0.5);

      double y;

      if (E = ((y = getWalkableHorizontalOffset(vector3.add(1, 0, 0))) != -256)) {
         Vector3 vec = vector3.add(1, y, 0);
         if (isPassable(vector3, vec) && !isContainsInClose(vec)) {
            Node nodeNear = getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, DIRECT_MOVE_COST + node.getG(), calHeuristic(vec, destination)));
            } else {
               if (node.getG() + DIRECT_MOVE_COST < nodeNear.getG()) {
                  nodeNear.setParent(node);
                  nodeNear.setG(node.getG() + DIRECT_MOVE_COST);
                  nodeNear.setF(nodeNear.getG() + nodeNear.getH());
               }
            }
         }
      }

      if (S = ((y = getWalkableHorizontalOffset(vector3.add(0, 0, 1))) != -256)) {
         Vector3 vec = vector3.add(0, y, 1);
         if (isPassable(vector3, vec) && !isContainsInClose(vec)) {
            Node nodeNear = getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, DIRECT_MOVE_COST + node.getG(), calHeuristic(vec, destination)));
            } else {
               if (node.getG() + DIRECT_MOVE_COST < nodeNear.getG()) {
                  nodeNear.setParent(node);
                  nodeNear.setG(node.getG() + DIRECT_MOVE_COST);
                  nodeNear.setF(nodeNear.getG() + nodeNear.getH());
               }
            }
         }
      }

      if (W = ((y = getWalkableHorizontalOffset(vector3.add(-1, 0, 0))) != -256)) {
         Vector3 vec = vector3.add(-1, y, 0);
         if (isPassable(vector3, vec) && !isContainsInClose(vec)) {
            Node nodeNear = getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, DIRECT_MOVE_COST + node.getG(), calHeuristic(vec, destination)));
            } else {
               if (node.getG() + DIRECT_MOVE_COST < nodeNear.getG()) {
                  nodeNear.setParent(node);
                  nodeNear.setG(node.getG() + DIRECT_MOVE_COST);
                  nodeNear.setF(nodeNear.getG() + nodeNear.getH());
               }
            }
         }
      }

      if (N = ((y = getWalkableHorizontalOffset(vector3.add(0, 0, -1))) != -256)) {
         Vector3 vec = vector3.add(0, y, -1);
         if (isPassable(vector3, vec) && !isContainsInClose(vec)) {
            Node nodeNear = getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, DIRECT_MOVE_COST + node.getG(), calHeuristic(vec, destination)));
            } else {
               if (node.getG() + DIRECT_MOVE_COST < nodeNear.getG()) {
                  nodeNear.setParent(node);
                  nodeNear.setG(node.getG() + DIRECT_MOVE_COST);
                  nodeNear.setF(nodeNear.getG() + nodeNear.getH());
               }
            }
         }
      }

      if (N && E && ((y = getWalkableHorizontalOffset(vector3.add(1, 0, -1))) != -256)) {
         Vector3 vec = vector3.add(1, y, -1);
         if (isPassable(vector3, vec) && !isContainsInClose(vec)) {
            Node nodeNear = getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, OBLIQUE_MOVE_COST + node.getG(), calHeuristic(vec, destination)));
            } else {
               if (node.getG() + OBLIQUE_MOVE_COST < nodeNear.getG()) {
                  nodeNear.setParent(node);
                  nodeNear.setG(node.getG() + OBLIQUE_MOVE_COST);
                  nodeNear.setF(nodeNear.getG() + nodeNear.getH());
               }
            }
         }
      }

      if (E && S && ((y = getWalkableHorizontalOffset(vector3.add(1, 0, 1))) != -256)) {
         Vector3 vec = vector3.add(1, y, 1);
         if (isPassable(vector3, vec) && !isContainsInClose(vec)) {
            Node nodeNear = getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, OBLIQUE_MOVE_COST + node.getG(), calHeuristic(vec, destination)));
            } else {
               if (node.getG() + OBLIQUE_MOVE_COST < nodeNear.getG()) {
                  nodeNear.setParent(node);
                  nodeNear.setG(node.getG() + OBLIQUE_MOVE_COST);
                  nodeNear.setF(nodeNear.getG() + nodeNear.getH());
               }
            }
         }
      }

      if (W && S && ((y = getWalkableHorizontalOffset(vector3.add(-1, 0, 1))) != -256)) {
         Vector3 vec = vector3.add(-1, y, 1);
         if (isPassable(vector3, vec) && !isContainsInClose(vec)) {
            Node nodeNear = getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, OBLIQUE_MOVE_COST + node.getG(), calHeuristic(vec, destination)));
            } else {
               if (node.getG() + OBLIQUE_MOVE_COST < nodeNear.getG()) {
                  nodeNear.setParent(node);
                  nodeNear.setG(node.getG() + OBLIQUE_MOVE_COST);
                  nodeNear.setF(nodeNear.getG() + nodeNear.getH());
               }
            }
         }
      }

      if (W && N && ((y = getWalkableHorizontalOffset(vector3.add(-1, 0, -1))) != -256)) {
         Vector3 vec = vector3.add(-1, y, -1);
         if (isPassable(vector3, vec) && !isContainsInClose(vec)) {
            Node nodeNear = getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, OBLIQUE_MOVE_COST + node.getG(), calHeuristic(vec, destination)));
            } else {
               if (node.getG() + OBLIQUE_MOVE_COST < nodeNear.getG()) {
                  nodeNear.setParent(node);
                  nodeNear.setG(node.getG() + OBLIQUE_MOVE_COST);
                  nodeNear.setF(nodeNear.getG() + nodeNear.getH());
               }
            }
         }
      }
   }

   private Node getNodeInOpenByVector2(Vector3 vector2) {
      for (Node node : this.openList) {
         if (vector2.equals(node.getVector3())) {
            return node;
         }
      }

      return null;
   }

   private boolean isContainsInOpen(Vector3 vector2) {
      return this.getNodeInOpenByVector2(vector2) != null;
   }

   private Node getNodeInCloseByVector2(Vector3 vector2) {
      for (Node node : this.closeList) {
         if (vector2.equals(node.getVector3())) {
            return node;
         }
      }
      return null;
   }

   private boolean isContainsInClose(Vector3 vector2) {
      return this.getNodeInCloseByVector2(vector2) != null;
   }

   private boolean hasBarrier(Vector3 pos1, Vector3 pos2) {
      if (pos1.equals(pos2)) {
         return false;
      }
      if (pos1.getFloorY() != pos2.getFloorY()) {
         return true;
      }
      boolean traverseDirection = Math.abs(pos1.getX() - pos2.getX()) > Math.abs(pos1.getZ() - pos2.getZ());
      if (traverseDirection) {
         double loopStart = Math.min(pos1.getX(), pos2.getX());
         double loopEnd = Math.max(pos1.getX(), pos2.getX());
         ArrayList<Vector3> list = new ArrayList<>();
         for (double i = Math.ceil(loopStart); i <= Math.floor(loopEnd); i += 1.0) {
            double result;
            if ((result = Utils.calLinearFunction(pos1, pos2, i, Utils.ACCORDING_X_OBTAIN_Y)) != Double.MAX_VALUE) {
               list.add(new Vector3(i, pos1.getY(), result));
            }
         }
         return hasBlocksAround(list);
      } else {
         double loopStart = Math.min(pos1.getZ(), pos2.getZ());
         double loopEnd = Math.max(pos1.getZ(), pos2.getZ());
         ArrayList<Vector3> list = new ArrayList<>();
         for (double i = Math.ceil(loopStart); i <= Math.floor(loopEnd); i += 1.0) {
            double result;
            if ((result = Utils.calLinearFunction(pos1, pos2, i, Utils.ACCORDING_Y_OBTAIN_X)) != Double.MAX_VALUE) {
               list.add(new Vector3(result, pos1.getY(), i));
            }
         }

         return hasBlocksAround(list);
      }

   }

   private boolean hasBlocksAround(ArrayList<Vector3> list) {
      double radius = (this.entity.getWidth() * this.entity.getScale()) / 2 + 0.1;
      double height = this.entity.getHeight() * this.entity.getScale();
      for (Vector3 vector3 : list) {
         AxisAlignedBB bb = new Area(vector3.getX() - radius, vector3.getY(), vector3.getZ() - radius, vector3.getX() + radius, vector3.getY() + height, vector3.getZ() + radius);
         if (this.level.getCollisionBlocks(bb, true).length != 0) {
            return true;
         }

         boolean xIsInt = vector3.getX() % 1 == 0;
         boolean zIsInt = vector3.getZ() % 1 == 0;
         if (xIsInt && zIsInt) {
            if (this.getBlockFast(new Vector3(vector3.getX(), vector3.getY() - 1, vector3.getZ()), false).canPassThrough() ||
                    this.getBlockFast(new Vector3(vector3.getX() - 1, vector3.getY() - 1, vector3.getZ()), false).canPassThrough() ||
                    this.getBlockFast(new Vector3(vector3.getX() - 1, vector3.getY() - 1, vector3.getZ() - 1), false).canPassThrough() ||
                    this.getBlockFast(new Vector3(vector3.getX(), vector3.getY() - 1, vector3.getZ() - 1), false).canPassThrough()) {
               return true;
            }
         } else if (xIsInt) {
            if (this.getBlockFast(new Vector3(vector3.getX(), vector3.getY() - 1, vector3.getZ()), false).canPassThrough() ||
                    this.getBlockFast(new Vector3(vector3.getX() - 1, vector3.getY() - 1, vector3.getZ()), false).canPassThrough()) {
               return true;
            }
         } else if (zIsInt) {
            if (this.getBlockFast(new Vector3(vector3.getX(), vector3.getY() - 1, vector3.getZ()), false).canPassThrough() ||
                    this.getBlockFast(new Vector3(vector3.getX(), vector3.getY() - 1, vector3.getZ() - 1), false).canPassThrough()) {
               return true;
            }
         } else {
            if (this.getBlockFast(new Vector3(vector3.getX(), vector3.getY() - 1, vector3.getZ()), false).canPassThrough()) {
               return true;
            }
         }
      }
      return false;
   }

   private ArrayList<Node> FloydSmooth(ArrayList<Node> array) {
      int current = 0;
      int total = 2;
      if (array.size() > 2) {
         while (total < array.size()) {
            if (!hasBarrier(array.get(current), array.get(total)) && total != array.size() - 1) {
               total++;
            } else {
               array.get(total - 1).setParent(array.get(current));
               current = total - 1;
               total++;
            }
         }

         Node temp = array.get(array.size() - 1);
         ArrayList<Node> tempL = new ArrayList<>();
         tempL.add(temp);
         while (temp.getParent() != null) {
            tempL.add((temp = temp.getParent()));
         }
         Collections.reverse(tempL);
         return tempL;
      }
      return array;
   }

   private ArrayList<Node> getPathRoute() {
      ArrayList<Node> nodes = new ArrayList<>();
      Node temp = this.closeList.get(this.closeList.size() - 1);
      nodes.add(temp);

      while(temp.getParent() != null && !temp.getParent().getVector3().equals(this.start)) {
         nodes.add(temp = temp.getParent());
      }

      if (temp.getParent() != null) {
         nodes.add(temp.getParent());
      }
      Collections.reverse(nodes);
      return nodes;
   }

   private boolean isPositionOverlap(Vector3 vector2, Vector3 vector2_) {
      if (vector2 == null || vector2_ == null) {
         return false;
      }
      return vector2.getFloorX() == vector2_.getFloorX() &&
              vector2.getFloorZ() == vector2_.getFloorZ() &&
              vector2.getFloorY() == vector2_.getFloorY();
   }

   public void resetTemporary() {
      this.openList.clear();
      this.closeList.clear();
      this.searchLimit = 100;
   }
}
