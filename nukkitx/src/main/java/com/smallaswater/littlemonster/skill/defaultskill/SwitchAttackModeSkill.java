package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.entity.Entity;
import com.smallaswater.littlemonster.entity.IEntity;
import com.smallaswater.littlemonster.skill.BaseSkillManager;

/**
 * 切换攻击模式
 *
 * @author LT_Name
 */
public class SwitchAttackModeSkill extends BaseSkillManager {

    public SwitchAttackModeSkill(String name) {
        super(name);
    }

    @Override
    protected void privateDisplay(Entity... entities) {
        for (Entity entity : entities) {
            if (entity instanceof IEntity) {
                ((IEntity) entity).setAttackMode(this.getEffect().intValue());
            }
        }
    }

}
