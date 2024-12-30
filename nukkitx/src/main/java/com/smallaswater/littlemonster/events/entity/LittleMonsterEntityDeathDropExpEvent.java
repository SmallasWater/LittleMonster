package com.smallaswater.littlemonster.events.entity;

import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import com.smallaswater.littlemonster.entity.IEntity;
import com.smallaswater.littlemonster.entity.baselib.BaseEntity;

/**
 * @author LT_Name
 */
public class LittleMonsterEntityDeathDropExpEvent extends LittleMonsterEntityEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    protected int dropExp;

    public LittleMonsterEntityDeathDropExpEvent(IEntity entity, int dropExp) {
        super(entity);
        this.dropExp = dropExp;
    }

    public void setDropExp(int dropExp) {
        this.dropExp = dropExp;
    }

    public int getDropExp() {
        return this.dropExp;
    }
}
