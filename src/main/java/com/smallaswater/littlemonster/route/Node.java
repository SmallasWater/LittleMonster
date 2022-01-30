package com.smallaswater.littlemonster.route;

import cn.nukkit.math.Vector3;
import java.util.Objects;

public class Node implements Comparable<Node> {
   private Vector3 vector3;
   private Node parent;
   private int G;
   private int H;
   private int F;

   Node(Vector3 vector3, Node parent, int G, int H) {
      this.vector3 = vector3;
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
      return this.vector3.toString() + "| G:" + this.G + " H:" + this.H + " F" + this.getF() + (this.parent != null ? "\tparent:" + String.valueOf(this.parent.getVector3()) : "");
   }

   public Vector3 getVector3() {
      return this.vector3;
   }

   public void setVector3(Vector3 vector3) {
      this.vector3 = vector3;
   }
}
