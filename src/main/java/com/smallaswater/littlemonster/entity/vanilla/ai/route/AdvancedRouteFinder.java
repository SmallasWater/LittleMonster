package com.smallaswater.littlemonster.entity.vanilla.ai.route;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockFence;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import com.smallaswater.littlemonster.entity.vanilla.ai.entity.Climbable;
import com.smallaswater.littlemonster.entity.vanilla.ai.entity.Fallable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdvancedRouteFinder extends RouteFinder{
	private boolean succeed = false, searching = false;

	private Vector3 realDestination = null;

	private Set<Node> open = new HashSet<>();

	private Grid grid = new Grid();

	public AdvancedRouteFinder(Entity entity){
		super(entity);
	}

	@Override
	public boolean search(){
		this.succeed = false;
		this.searching = true;

		if(this.getStart() == null || this.getDestination() == null){
			return this.succeed = this.searching = false;
		}
		this.resetNodes();
		Node start = new Node(this.getStart().floor());
		Node endNode = new Node(this.realDestination.floor());
		try {
			start.f = start.g = 0;
			open.add(start);
			this.grid.putNode(start.getVector3(), start);
			this.grid.putNode(endNode.getVector3(), endNode);
		} catch (Exception e) {
			return this.succeed = this.searching = false;
		}

		int limit = searchLimit;
		Node node = null;
		Node tmp = null;
		Node previous = null;
		while(!open.isEmpty() && limit-- > 0){
			if (this.forceStop) {
				this.resetNodes();
				this.forceStop = false;
				return this.succeed = this.searching = false;
			}

			node = null;

			double f = Double.MAX_VALUE;
			try {
				for(Node cur : this.open){
					if(cur.f < f && cur.f != -1){
						node = cur;
						f = cur.f;
					}
				}
			} catch (Exception e) {
				return this.succeed = this.searching = false;
			}

			if(endNode.equals(node)){
				tmp = node;
				while((tmp = tmp.getParent()) != null){
					tmp.add(0.5,0,0.5);
				}
				List<Node> nodes = new ArrayList<>();
				nodes.add(node);
				while((node = node.getParent()) != null){
					previous = node;
					if(((int)node.getX())==node.getX() && ((int)node.getZ())==node.getZ()){
						previous.setParent(node.getParent());
					}else {
						nodes.add(node);
					}
				}
//				nodes.remove(nodes.size()-1);
//				if(nodes.size()!=0){
//					Node p = nodes.get(nodes.size()-1);
//					Node a = new Node((p.getX()+entity.getX())/2,entity.getY(),(p.getZ()+entity.getZ())/2);
//					nodes.add(a);
//				}
				Collections.reverse(nodes);

				super.nodes = nodes;
//				nodes.forEach(n->level.addParticle(new cn.nukkit.level.particle.CriticalParticle(n.getVector3().add(0,0.15),3)));
				this.succeed = true; this.searching = false;
				return true;
			}

			node.closed = true;
			open.remove(node);

			for(Node neighbor : this.getNeighbors(node)){// 获取节点的邻里方块并遍历
				if(neighbor.closed) continue;

				double tentative_gScore = node.g + neighbor.getVector3().distance(node.getVector3());

				if(!open.contains(neighbor)) open.add(neighbor);
				else if(neighbor.g != -1 && tentative_gScore >= neighbor.g) continue;

				neighbor.setParent(node);
				neighbor.g = tentative_gScore;
				neighbor.f = neighbor.g + this.heuristic(neighbor.getVector3(), endNode.getVector3());

				if (this.forceStop) {
					this.resetNodes();
					this.forceStop = false;
					return this.succeed = this.searching = false;
				}
			}
		}

		return this.succeed = this.searching = false;
	}

	public Set<Node> getNeighbors(Node node){
		Set<Node> neighbors = new HashSet<>();
		Vector3 vec = node.getVector3();

		boolean s1, s2, s3, s4;

		double y;
		Node toAdd;
		if(s1 = (y = isWalkableAt(vec.add(1))) != -256){
			toAdd = this.grid.getNode(vec.add(1, y));
			if(!isJamNode(toAdd)) neighbors.add(toAdd);
		}

		if(s2 = (y = isWalkableAt(vec.add(-1))) != -256){
			toAdd = this.grid.getNode(vec.add(-1, y));
			if(!isJamNode(toAdd)) neighbors.add(toAdd);
		}

		if(s3 = (y = isWalkableAt(vec.add(0, 0, 1))) != -256){
			toAdd = this.grid.getNode(vec.add(0, y, 1));
			if(!isJamNode(toAdd)) neighbors.add(toAdd);
		}

		if(s4 = (y = isWalkableAt(vec.add(0, 0, -1))) != -256){
			toAdd = this.grid.getNode(vec.add(0, y, -1));
			if(!isJamNode(toAdd)) neighbors.add(toAdd);
		}

		if(s1 && s3 && (y = isWalkableAt(vec.add(1, 0, 1))) != -256 &&
				canPassThroughBlock(this.level.getBlock(vec.add(1, y, 0))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(0, y, 1))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(1, y+1, 0))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(0, y+1, 1)))){
			toAdd = (this.grid.getNode(vec.add(1, y, 1)));
			if(!isJamNode(toAdd)) neighbors.add(toAdd);
		}

		if(s1 && s4 && (y = isWalkableAt(vec.add(1, 0, -1))) != -256 &&
				canPassThroughBlock(this.level.getBlock(vec.add(1, y, 0))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(0, y, -1))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(1, y+1, 0))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(0, y+1, -1)))){
			toAdd = (this.grid.getNode(vec.add(1, y, -1)));
			if(!isJamNode(toAdd)) neighbors.add(toAdd);
		}

		if(s2 && s3 && (y = isWalkableAt(vec.add(-1, 0, 1))) != -256 &&
				canPassThroughBlock(this.level.getBlock(vec.add(-1, y, 0))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(0, y, 1))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(-1, y+1, 0))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(0, y+1, 1)))){
			toAdd = (this.grid.getNode(vec.add(-1, y, 1)));
			if(!isJamNode(toAdd)) neighbors.add(toAdd);
		}

		if(s2 && s4 && (y = isWalkableAt(vec.add(-1, 0, -1))) != -256 &&
				canPassThroughBlock(this.level.getBlock(vec.add(-1, y, 0))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(0, y, -1))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(-1, y+1, 0))) &&
				canPassThroughBlock(this.level.getBlock(vec.add(0, y+1, -1)))){
			toAdd = (this.grid.getNode(vec.add(-1, y, -1)));
			if(!isJamNode(toAdd)) neighbors.add(toAdd);
		}

		return neighbors;
	}

	public static boolean canPassThroughBlock(Block block){
		switch (block.getId()){
			case 6: case 31: case 32: case 37: case 38: case 39: case 40: case 50: case 55: case 59:
			case 83: case 93: case 94: case 141: case 142: case 147: case 148: case 149: case 150:
			case 171: case 175: case 106: case 0:
				return true;
			default:
				return block.canPassThrough();
		}
	}

	public boolean isJamNode(Node node){
		return false;
//		if(node.getX() == (int) node.getX() && node.getY() == (int) node.getY() && node.getZ() == (int) node.getZ()){
//			if(node.getVector3().distance(destination)<1.5 || node.getVector3().distance(start)<1.5){
//				return false;
//			}else{
//				boolean re = !(canPassThroughBlock(this.level.getBlock((int)node.getX(),(int)node.getY(),(int)node.getZ())) &&
//						canPassThroughBlock(this.level.getBlock((int)node.getX()-1,(int)node.getY(),(int)node.getZ())) &&
//						canPassThroughBlock(this.level.getBlock((int)node.getX(),(int)node.getY(),(int)node.getZ()-1)) &&
//						canPassThroughBlock(this.level.getBlock((int)node.getX()-1,(int)node.getY(),(int)node.getZ()-1)));
//				//if(re)level.addParticle(new cn.nukkit.level.particle.FlameParticle(node.getVector3().add(0,0.15)));
//				return re;
//			}
////						canPassThroughBlock(this.level.getBlock((int)node.getX()-1,(int)node.getY(),(int)node.getZ()+1)) &&
////						canPassThroughBlock(this.level.getBlock((int)node.getX()-1,(int)node.getY(),(int)node.getZ()-1)) &&
////						canPassThroughBlock(this.level.getBlock((int)node.getX()+1,(int)node.getY(),(int)node.getZ()-1)) &&
////						canPassThroughBlock(this.level.getBlock((int)node.getX()+1,(int)node.getY(),(int)node.getZ())) &&
////						canPassThroughBlock(this.level.getBlock((int)node.getX()+1,(int)node.getY(),(int)node.getZ()+1)));
//		}else {
//			return false;
//		}
	}

	private Block getHighestUnder(double x, double dy, double z){
		for(int y=(int)dy;y >= 0; y--){
			Block block = level.getBlock(new Vector3(x, y, z));

			if(!canWalkOn(block)) return block;
			if(!canPassThroughBlock(block)) return block;
		}
		return null;
	}

	public final double isWalkableAt(Vector3 vec){
		Block block = this.getHighestUnder(vec.x, vec.y + 1, vec.z);
		if(block == null) return -256;

		double diff = (block.y - vec.y) + 1;

		if((this.entity instanceof Fallable || -4 < diff) && (this.entity instanceof Climbable || diff <= 1) && canWalkOn(block)){
			return diff;
		}
		return -256;
	}

	private boolean canWalkOn(Block block){
		return !(block.getId() == Block.LAVA || block.getId() == Block.STILL_LAVA);
	}

	private double heuristic(Vector3 one, Vector3 two){
		double dx = Math.abs(one.x - two.x);
		double dy = Math.abs(one.y - two.y);
		double dz = Math.abs(one.z - two.z);

		double max = Math.max(dx, dz);
		double min = Math.min(dx, dz);
		return 0.414 * min + max + dy;
	}

	@Override
	public void resetNodes(){
		super.resetNodes();
		this.grid.clear();
		if (this.destination != null) {
			Vector3 block = this.getHighestUnder(this.destination.x, this.destination.y, this.destination.z);
			if(block == null){
				block = new Vector3(this.destination.x, 0, this.destination.z);
			}
			this.realDestination = new Vector3(this.destination.x, block.y + 1, this.destination.z).floor();
		}
	}

	@Override
	public boolean research(){
		this.resetNodes();

		return this.search();
	}

	@Override
	public boolean isSearching(){
		return this.searching;
	}

	@Override
	public boolean isSuccess(){
		return this.succeed;
	}

	private class Grid{
		private Map<Double, Map<Double, Map<Double, Node>>> grid = new HashMap<>();

		public void clear(){
			grid.clear();
		}

		public void putNode(Vector3 vec, Node node){
			vec = vec.floor();

			if(!grid.containsKey(vec.x)){
				grid.put(vec.x, new HashMap<>());
			}

			if(!grid.get(vec.x).containsKey(vec.y)){
				grid.get(vec.x).put(vec.y, new HashMap<>());
			}

			grid.get(vec.x).get(vec.y).put(vec.z, node);
		}

		public Node getNode(Vector3 vec){
			vec = vec.floor();

			if(!grid.containsKey(vec.x) || !grid.get(vec.x).containsKey(vec.y) || !grid.get(vec.x).get(vec.y).containsKey(vec.z)){
				Node node = new Node(vec.x, vec.y, vec.z);
				this.putNode(node.getVector3(), node);
				return node;
			}

			return grid.get(vec.x).get(vec.y).get(vec.z);
		}
	}
}
