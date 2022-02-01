package com.smallaswater.littlemonster.route;

import cn.nukkit.block.Block;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.math.Vector3;
import com.smallaswater.littlemonster.entity.baselib.BaseEntity;
import com.smallaswater.littlemonster.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.PriorityQueue;


public class WalkerRouteFinder extends SimpleRouteFinder {

   private static final int DIRECT_MOVE_COST = 10;

   private static final int OBLIQUE_MOVE_COST = 14;

   private PriorityQueue<Node> openList = new PriorityQueue<>();

   private ArrayList<Node> closeList = new ArrayList<>();

   private int searchLimit = 100;

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
      this.destination = destination;
   }

   private int calHeuristic(Vector3 pos1, Vector3 pos2) {
      return 10 * (Math.abs(pos1.getFloorX() - pos2.getFloorX()) + Math.abs(pos1.getFloorZ() - pos2.getFloorZ())) + 11 * Math.abs(pos1.getFloorY() - pos2.getFloorY());
   }

   @Override
   public boolean search() {
      if (this.entity.isClosed() || this.entity.getFollowTarget() == null ) {
         this.searching = false;
         this.finished = true;
         return false;
      }

      this.finished = false;
      this.searching = true;
      if (this.start == null) {
         this.start = this.entity;
      }

      if (this.destination == null) {
         if (this.entity.getFollowTarget() == null || this.entity.getFollowTarget().isClosed()) {
            this.searching = false;
            this.finished = true;
            return false;
         }

         this.destination = this.entity.getTargetVector().clone();
      }

      if (this.destinationDeviate > 0) {
         double x = Utils.rand(this.destinationDeviate - 3, this.destinationDeviate);
         double z = Utils.rand(this.destinationDeviate - 3, this.destinationDeviate);
         Vector3 vector3 = this.destination.add(Utils.rand() ? x : -x, 0, Utils.rand() ? z : -z);
         vector3.y += 5;
         for (int i=0; i<10; i++) {
            if (this.level.getBlock(vector3).canPassThrough() && !this.level.getBlock(vector3.down()).canPassThrough()) {
               this.destination = vector3;
               break;
            }
            vector3.y--;
         }
      }

      this.resetTemporary();
      Node presentNode = new Node(this.start);
      this.closeList.add(new Node(this.start));

      while(!this.isPositionOverlap(presentNode.getVector3(), this.destination)) {
         if (this.isInterrupted()) {
            this.searchLimit = 0;
            this.searching = false;
            this.finished = true;
            return false;
         }

         this.putNeighborNodeIntoOpen(presentNode);
         if (this.openList.peek() == null || this.searchLimit-- <= 0) {
            this.searching = false;
            this.finished = true;
            this.reachable = false;
            this.addNode(new Node(this.destination));
            return false;
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
      return true;
   }

   private Block getHighestUnder(Vector3 vector3, int limit) {
      int y;
      Block block;
      if (limit > 0) {
         for(y = vector3.getFloorY(); y >= vector3.getFloorY() - limit; --y) {
            block = this.level.getBlock(vector3.getFloorX(), y, vector3.getFloorZ(), false);
            if (this.isWalkable(block) && this.level.getBlock(block.add(0.0D, 1.0D, 0.0D), false).getId() == 0) {
               return block;
            }
         }

         return null;
      } else {
         for(y = vector3.getFloorY(); y >= 0; --y) {
            block = this.level.getBlock(vector3.getFloorX(), y, vector3.getFloorZ(), false);
            if (this.isWalkable(block) && this.level.getBlock(block.add(0.0D, 1.0D, 0.0D), false).getId() == 0) {
               return block;
            }
         }

         return null;
      }
   }

   private boolean canWalkOn(Block block) {
      return block.getId() != 10 && block.getId() != 11 && block.getId() != 81;
   }

   private boolean isWalkable(Vector3 vector3) {
      Block block = this.level.getBlock(vector3, false);
      return !block.canPassThrough() && this.canWalkOn(block);
   }

   private boolean isPassable(Vector3 vector3) {
      double radius = this.entity.getWidth() * this.entity.getScale() / 2.0F;
      float height = this.entity.getHeight() * this.entity.getScale();
      AxisAlignedBB bb = new SimpleAxisAlignedBB(vector3.getX() - radius, vector3.getY(), vector3.getZ() - radius, vector3.getX() + radius, vector3.getY() + (double)height, vector3.getZ() + radius);
      return this.level.getCollisionBlocks(bb, true).length == 0 && !this.level.getBlock(vector3.add(0.0D, -1.0D, 0.0D), false).canPassThrough();
   }

   private int getWalkableHorizontalOffset(Vector3 vector3) {
      Block block = this.getHighestUnder(vector3, 4);
      return block != null ? block.getFloorY() - vector3.getFloorY() + 1 : -256;
   }

   public int getSearchLimit() {
      return this.searchLimit;
   }

   public void setSearchLimit(int limit) {
      this.searchLimit = limit;
   }

   private void putNeighborNodeIntoOpen(Node node) {
      Vector3 vector3 = new Vector3((double)node.getVector3().getFloorX() + 0.5D, node.getVector3().getY(), (double)node.getVector3().getFloorZ() + 0.5D);
      boolean E;
      double y;
      Vector3 vec;
      Node nodeNear;
      if (E = (y = (double)this.getWalkableHorizontalOffset(vector3.add(1.0D, 0.0D, 0.0D))) != -256.0D) {
         vec = vector3.add(1.0D, y, 0.0D);
         if (this.isPassable(vec) && !this.isContainsInClose(vec)) {
            nodeNear = this.getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, 10 + node.getG(), this.calHeuristic(vec, this.destination)));
            } else if (node.getG() + 10 < nodeNear.getG()) {
               nodeNear.setParent(node);
               nodeNear.setG(node.getG() + 10);
               nodeNear.setF(nodeNear.getG() + nodeNear.getH());
            }
         }
      }

      boolean S;
      if (S = (y = (double)this.getWalkableHorizontalOffset(vector3.add(0.0D, 0.0D, 1.0D))) != -256.0D) {
         vec = vector3.add(0.0D, y, 1.0D);
         if (this.isPassable(vec) && !this.isContainsInClose(vec)) {
            nodeNear = this.getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, 10 + node.getG(), this.calHeuristic(vec, this.destination)));
            } else if (node.getG() + 10 < nodeNear.getG()) {
               nodeNear.setParent(node);
               nodeNear.setG(node.getG() + 10);
               nodeNear.setF(nodeNear.getG() + nodeNear.getH());
            }
         }
      }

      boolean W;
      if (W = (y = (double)this.getWalkableHorizontalOffset(vector3.add(-1.0D, 0.0D, 0.0D))) != -256.0D) {
         vec = vector3.add(-1.0D, y, 0.0D);
         if (this.isPassable(vec) && !this.isContainsInClose(vec)) {
            nodeNear = this.getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, 10 + node.getG(), this.calHeuristic(vec, this.destination)));
            } else if (node.getG() + 10 < nodeNear.getG()) {
               nodeNear.setParent(node);
               nodeNear.setG(node.getG() + 10);
               nodeNear.setF(nodeNear.getG() + nodeNear.getH());
            }
         }
      }

      boolean N;
      if (N = (y = (double)this.getWalkableHorizontalOffset(vector3.add(0.0D, 0.0D, -1.0D))) != -256.0D) {
         vec = vector3.add(0.0D, y, -1.0D);
         if (this.isPassable(vec) && !this.isContainsInClose(vec)) {
            nodeNear = this.getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, 10 + node.getG(), this.calHeuristic(vec, this.destination)));
            } else if (node.getG() + 10 < nodeNear.getG()) {
               nodeNear.setParent(node);
               nodeNear.setG(node.getG() + 10);
               nodeNear.setF(nodeNear.getG() + nodeNear.getH());
            }
         }
      }

      if (N && E && (y = (double)this.getWalkableHorizontalOffset(vector3.add(1.0D, 0.0D, -1.0D))) != -256.0D) {
         vec = vector3.add(1.0D, y, -1.0D);
         if (this.isPassable(vec) && !this.isContainsInClose(vec)) {
            nodeNear = this.getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, 14 + node.getG(), this.calHeuristic(vec, this.destination)));
            } else if (node.getG() + 14 < nodeNear.getG()) {
               nodeNear.setParent(node);
               nodeNear.setG(node.getG() + 14);
               nodeNear.setF(nodeNear.getG() + nodeNear.getH());
            }
         }
      }

      if (E && S && (y = (double)this.getWalkableHorizontalOffset(vector3.add(1.0D, 0.0D, 1.0D))) != -256.0D) {
         vec = vector3.add(1.0D, y, 1.0D);
         if (this.isPassable(vec) && !this.isContainsInClose(vec)) {
            nodeNear = this.getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, 14 + node.getG(), this.calHeuristic(vec, this.destination)));
            } else if (node.getG() + 14 < nodeNear.getG()) {
               nodeNear.setParent(node);
               nodeNear.setG(node.getG() + 14);
               nodeNear.setF(nodeNear.getG() + nodeNear.getH());
            }
         }
      }

      if (W && S && (y = (double)this.getWalkableHorizontalOffset(vector3.add(-1.0D, 0.0D, 1.0D))) != -256.0D) {
         vec = vector3.add(-1.0D, y, 1.0D);
         if (this.isPassable(vec) && !this.isContainsInClose(vec)) {
            nodeNear = this.getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, 14 + node.getG(), this.calHeuristic(vec, this.destination)));
            } else if (node.getG() + 14 < nodeNear.getG()) {
               nodeNear.setParent(node);
               nodeNear.setG(node.getG() + 14);
               nodeNear.setF(nodeNear.getG() + nodeNear.getH());
            }
         }
      }

      if (W && N && (y = (double)this.getWalkableHorizontalOffset(vector3.add(-1.0D, 0.0D, -1.0D))) != -256.0D) {
         vec = vector3.add(-1.0D, y, -1.0D);
         if (this.isPassable(vec) && !this.isContainsInClose(vec)) {
            nodeNear = this.getNodeInOpenByVector2(vec);
            if (nodeNear == null) {
               this.openList.offer(new Node(vec, node, 14 + node.getG(), this.calHeuristic(vec, this.destination)));
            } else if (node.getG() + 14 < nodeNear.getG()) {
               nodeNear.setParent(node);
               nodeNear.setG(node.getG() + 14);
               nodeNear.setF(nodeNear.getG() + nodeNear.getH());
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

   private boolean hasBarrier(Node node1, Node node2) {
      return this.hasBarrier(node1.getVector3(), node2.getVector3());
   }

   private boolean hasBarrier(Vector3 pos1, Vector3 pos2) {
      if (pos1.equals(pos2)) {
         return false;
      } else if (pos1.getFloorY() != pos2.getFloorY()) {
         return true;
      } else {
         boolean traverseDirection = Math.abs(pos1.getX() - pos2.getX()) > Math.abs(pos1.getZ() - pos2.getZ());
         double loopStart;
         double loopEnd;
         ArrayList list;
         double i;
         double result;
         if (traverseDirection) {
            loopStart = Math.min(pos1.getX(), pos2.getX());
            loopEnd = Math.max(pos1.getX(), pos2.getX());
            list = new ArrayList();

            for(i = Math.ceil(loopStart); i <= Math.floor(loopEnd); ++i) {
               if ((result = Utils.calLinearFunction(pos1, pos2, i, 0)) != Double.MAX_VALUE) {
                  list.add(new Vector3(i, pos1.getY(), result));
               }
            }

            return this.hasBlocksAround(list);
         } else {
            loopStart = Math.min(pos1.getZ(), pos2.getZ());
            loopEnd = Math.max(pos1.getZ(), pos2.getZ());
            list = new ArrayList();

            for(i = Math.ceil(loopStart); i <= Math.floor(loopEnd); ++i) {
               if ((result = Utils.calLinearFunction(pos1, pos2, i, 1)) != Double.MAX_VALUE) {
                  list.add(new Vector3(result, pos1.getY(), i));
               }
            }

            return this.hasBlocksAround(list);
         }
      }
   }

   private boolean hasBlocksAround(ArrayList<Vector3> list) {
      double radius = (double)(this.entity.getWidth() * this.entity.getScale() / 2.0F) + 0.1D;
      double height = (double)(this.entity.getHeight() * this.entity.getScale());
      Iterator var6 = list.iterator();

      while(var6.hasNext()) {
         Vector3 vector3 = (Vector3)var6.next();
         AxisAlignedBB bb = new SimpleAxisAlignedBB(vector3.getX() - radius, vector3.getY(), vector3.getZ() - radius, vector3.getX() + radius, vector3.getY() + height, vector3.getZ() + radius);
         if (this.level.getCollisionBlocks(bb, true).length != 0) {
            return true;
         }

         boolean xIsInt = vector3.getX() % 1.0D == 0.0D;
         boolean zIsInt = vector3.getZ() % 1.0D == 0.0D;
         if (xIsInt && zIsInt) {
            if (this.level.getBlock(new Vector3(vector3.getX(), vector3.getY() - 1.0D, vector3.getZ()), false).canPassThrough() || this.level.getBlock(new Vector3(vector3.getX() - 1.0D, vector3.getY() - 1.0D, vector3.getZ()), false).canPassThrough() || this.level.getBlock(new Vector3(vector3.getX() - 1.0D, vector3.getY() - 1.0D, vector3.getZ() - 1.0D), false).canPassThrough() || this.level.getBlock(new Vector3(vector3.getX(), vector3.getY() - 1.0D, vector3.getZ() - 1.0D), false).canPassThrough()) {
               return true;
            }
         } else if (xIsInt) {
            if (this.level.getBlock(new Vector3(vector3.getX(), vector3.getY() - 1.0D, vector3.getZ()), false).canPassThrough() || this.level.getBlock(new Vector3(vector3.getX() - 1.0D, vector3.getY() - 1.0D, vector3.getZ()), false).canPassThrough()) {
               return true;
            }
         } else if (zIsInt) {
            if (this.level.getBlock(new Vector3(vector3.getX(), vector3.getY() - 1.0D, vector3.getZ()), false).canPassThrough() || this.level.getBlock(new Vector3(vector3.getX(), vector3.getY() - 1.0D, vector3.getZ() - 1.0D), false).canPassThrough()) {
               return true;
            }
         } else if (this.level.getBlock(new Vector3(vector3.getX(), vector3.getY() - 1.0D, vector3.getZ()), false).canPassThrough()) {
            return true;
         }
      }

      return false;
   }

   private ArrayList<Node> FloydSmooth(ArrayList<Node> array) {
      int current = 0;
      int total = 2;
      if (array.size() <= 2) {
         return array;
      } else {
         while(true) {
            while(total < array.size()) {
               if (!this.hasBarrier(array.get(current), array.get(total)) && total != array.size() - 1) {
                  ++total;
               } else {
                  array.get(total - 1).setParent(array.get(current));
                  current = total - 1;
                  ++total;
               }
            }

            Node temp = array.get(array.size() - 1);
            ArrayList<Node> tempL = new ArrayList<>();
            tempL.add(temp);

            while(temp.getParent() != null) {
               tempL.add(temp = temp.getParent());
            }

            Collections.reverse(tempL);
            return tempL;
         }
      }
   }

   private ArrayList<Node> getPathRoute() {
      ArrayList<Node> nodes = new ArrayList<>();
      Node temp = this.closeList.get(this.closeList.size() - 1);
      nodes.add(temp);

      while(!temp.getParent().getVector3().equals(this.start)) {
         nodes.add(temp = temp.getParent());
      }

      nodes.add(temp.getParent());
      Collections.reverse(nodes);
      return nodes;
   }

   private boolean isPositionOverlap(Vector3 vector2, Vector3 vector2_) {
      if (vector2 == null || vector2_ == null) {
         return false;
      }
      return vector2.getFloorX() == vector2_.getFloorX() && vector2.getFloorZ() == vector2_.getFloorZ() && vector2.getFloorY() == vector2_.getFloorY();
   }

   public void resetTemporary() {
      this.openList.clear();
      this.closeList.clear();
      this.searchLimit = 100;
   }
}
