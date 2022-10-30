package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import com.smallaswater.littlemonster.utils.Utils;

import java.util.ArrayList;

/**
 * @author SmallasWater
 * Create on 2021/6/29 20:08
 * Package com.smallaswater.littlemonster.skill.defaultskill
 */
public class SummonHealthSkill extends BaseSkillManager {

    private ArrayList<String> littleNpcs = new ArrayList<>();

    public SummonHealthSkill(String name) {
        super(name);
    }

    public void setLittleNpcs(ArrayList<String> littleNpcs) {
        this.littleNpcs = littleNpcs;
    }

    @Override
    public void display(Entity... player) {
        for(String littleNpcName: littleNpcs){
            MonsterConfig config = LittleMonsterMainClass.getInstance().monsters.get(littleNpcName);
            if(config != null){
                LittleNpc littleNpc = config.spawn(getMaster().add(Utils.rand()?1:-1,0,Utils.rand()?1:-1));
                littleNpc.isToDeath = true;
                littleNpc.setMasterHuman((EntityHuman) player[0]);
            }

        }
    }
}
