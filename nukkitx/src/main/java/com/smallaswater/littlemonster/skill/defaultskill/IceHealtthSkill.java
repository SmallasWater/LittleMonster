package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import com.smallaswater.littlemonster.manager.KeyHandleManager;
import com.smallaswater.littlemonster.skill.BaseSkillAreaManager;
import com.smallaswater.littlemonster.skill.BaseSkillManager;

/**
 * @author SmallasWater
 * Create on 2021/6/29 20:06
 * Package com.smallaswater.littlemonster.skill.defaultskill
 */
public class IceHealtthSkill extends BaseSkillManager implements BaseSkillAreaManager {

    public IceHealtthSkill(String name) {
        super(name);
    }

    @Override
    protected void privateDisplay(Entity... entities) {
        for (Entity entity : entities) {
            if (entity instanceof Player) {
                KeyHandleManager.addTimmerKey((Player) entity, getName(), getEffect().intValue());
            }
        }

    }
}
