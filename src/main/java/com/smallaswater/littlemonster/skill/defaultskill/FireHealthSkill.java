package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.entity.Entity;
import com.smallaswater.littlemonster.skill.BaseSkillAreaManager;
import com.smallaswater.littlemonster.skill.BaseSkillManager;

/**
 * @author SmallasWater
 * Create on 2021/6/29 20:06
 * Package com.smallaswater.littlemonster.skill.defaultskill
 */
public class FireHealthSkill extends BaseSkillManager implements BaseSkillAreaManager {



    public FireHealthSkill(String name) {
        super(name);
    }

    @Override
    public void display(Entity... player) {
        for(Entity player1: player){
            player1.setOnFire(getEffect().intValue());
        }
    }


}
