package com.smallaswater.littlemonster.events.entity;

import cn.nukkit.event.Cancellable;
import com.smallaswater.littlemonster.entity.baselib.BaseEntity;

/**
 * @author LT_Name
 */
public class LittleMonsterEntityDeathDropExpEvent extends LittleMonsterEntityEvent implements Cancellable {

    protected int dropExp;

    public LittleMonsterEntityDeathDropExpEvent(BaseEntity entity, int dropExp) {
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
