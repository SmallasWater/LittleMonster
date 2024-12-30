package com.smallaswater.littlemonster.route;

import cn.nukkit.math.Vector3;

import java.util.Objects;

public class Node extends Vector3 implements Comparable<Node> {

   private Node parent;

   private int G;

   private int H;

   private int F;

   Node(Vector3 vector3, Node parent, int G, int H) {
      super(vector3.x, vector3.y, vector3.z);
      this.parent = parent;
      this.G = G;
      this.H = H;
      this.F = G + H;
   }

   Node(Vector3 vector3) {
      this(vector3, (Node)null, 0, 0);
   }

   public Node getParent() {
      return this.parent;
   }

   public void setParent(Node parent) {
      this.parent = parent;
   }

   public int getG() {
      return this.G;
   }

   public void setG(int g) {
      this.G = g;
   }

   public int getH() {
      return this.H;
   }

   public void setH(int h) {
      this.H = h;
   }

   public int getF() {
      return this.F;
   }

   public void setF(int f) {
      this.F = f;
   }

   public int compareTo(Node o) {
      Objects.requireNonNull(o);
      if (this.getF() != o.getF()) {
         return this.getF() - o.getF();
      } else {
         double breaking;
         if ((breaking = (double)this.getG() + (double)this.getH() * 0.1D - ((double)o.getG() + (double)this.getH() * 0.1D)) > 0.0D) {
            return 1;
         } else {
            return breaking < 0.0D ? -1 : 0;
         }
      }
   }

   public String toString() {
      return super.toString() + "| G:" + this.G + " H:" + this.H + " F" + this.getF() + (this.parent != null ? "\tparent:" + this.parent.getVector3() : "");
   }

   public Vector3 getVector3() {
      return this;
   }

   public void setVector3(Vector3 vector3) {
      this.setX(vector3.x);
      this.setY(vector3.y);
      this.setZ(vector3.z);
   }
}
