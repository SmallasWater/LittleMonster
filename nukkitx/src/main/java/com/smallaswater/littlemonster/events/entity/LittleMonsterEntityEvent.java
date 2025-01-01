package com.smallaswater.littlemonster.events.entity;

import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityEvent;
import com.smallaswater.littlemonster.entity.IEntity;
import com.smallaswater.littlemonster.entity.baselib.BaseEntity;

/**
 * @author LT_Name
 */
public abstract class LittleMonsterEntityEvent extends EntityEvent {

    IEntity ientity;
    protected LittleMonsterEntityEvent(IEntity entity) {
        ientity = entity;
    }

    @Override
    public Entity getEntity() {
        return ientity.getEntity();
    }
}
