package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
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
    public void display(Entity... player) {

        for(Entity player1: player){
            if(player1 instanceof Player) {
                KeyHandleManager.addTimmerKey((Player) player1, getName(), getEffect().intValue());
            }
        }

    }
}
