package com.smallaswater.littlemonster.entity.vanilla.ai.route;

import com.smallaswater.littlemonster.entity.vanilla.ai.entity.MovingEntity;

public class SimpleRouteFinder extends RouteFinder{
	public SimpleRouteFinder(MovingEntity entity){
		super(entity);
	}

	@Override
	public boolean search(){
		this.resetNodes();

		this.addNode(new Node(this.getDestination())); // just go straight

		return true;
	}

	@Override
	public boolean research(){
		return this.search();
	}

	@Override
	public boolean isSearching(){
		return false;
	}

	@Override
	public boolean isSuccess(){
		return nodes.size() > 0;
	}
}
