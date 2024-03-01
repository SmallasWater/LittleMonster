package com.smallaswater.littlemonster.events.entity;

import cn.nukkit.event.entity.EntityEvent;
import com.smallaswater.littlemonster.entity.baselib.BaseEntity;

/**
 * @author LT_Name
 */
public abstract class LittleMonsterEntityEvent extends EntityEvent {

    protected LittleMonsterEntityEvent(BaseEntity entity) {
        this.entity = entity;
    }

    @Override
    public BaseEntity getEntity() {
        return (BaseEntity) super.getEntity();
    }
}
