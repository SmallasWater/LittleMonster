package com.smallaswater.littlemonster.entity.vanilla.ai.route;

import cn.nukkit.math.Vector3;

public class Node implements Cloneable{
	private Node parent = null;
	public boolean closed = false;

	private Vector3 node;
	public double f = -1;
	public double g = -1;

	public Node(double x, double y, double z){
		this.node = new Vector3(x, y, z);
	}

	public Node(Vector3 vec){
		if(vec == null){
			throw new IllegalArgumentException("Node cannot be null");
		}

		this.node = new Vector3(vec.x, vec.y, vec.z);
	}

	public Vector3 getVector3(){
		return new Vector3(node.x, node.y, node.z);
	}

	public Vector3 getRawVector3(){
		return this.node;
	}

	public double getX(){
		return this.node.x;
	}

	public double getY(){
		return this.node.y;
	}

	public double getZ(){
		return this.node.z;
	}

	public Node add(double x, double y, double z){
		this.node = this.node.add(x, y, z);
		return this;
	}

	public String toString(){
		return "Node (x=" + this.node.x + ", y=" + this.node.y + ", " + this.node.z + ")";
	}

	public void setParent(Node node){
		this.parent = node;
	}

	public Node getParent(){
		return this.parent;
	}

	public double getParentDx(){
		return this.parent.getX()-this.getX();
	}

	public double getParentDy(){
		return this.parent.getY()-this.getY();
	}

	public double getParentDz(){
		return this.parent.getZ()-this.getZ();
	}

	public Node getGrandParent(){
		return this.parent==null?null:this.parent.parent;
	}

	public double getGrandParentDx(){
		return this.getGrandParent().getX()-this.getX();
	}

	public double getGrandParentDy(){
		return this.getGrandParent().getY()-this.getY();
	}

	public double getGrandParentDz(){
		return this.getGrandParent().getZ()-this.getZ();
	}

	@Override
	public Node clone(){
		return new Node(this.getVector3());
	}

	public boolean equals(Node node){
		return (int)this.getX() == (int)node.getX() && (int)this.getY() == (int)node.getY() && (int)this.getZ() == (int)node.getZ();
	}
}
