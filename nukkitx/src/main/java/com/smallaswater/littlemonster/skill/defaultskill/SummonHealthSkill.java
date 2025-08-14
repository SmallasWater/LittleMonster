package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.IEntity;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import com.smallaswater.littlemonster.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/**
 * @author SmallasWater
 * Create on 2021/6/29 20:08
 * Package com.smallaswater.littlemonster.skill.defaultskill
 */
@Setter
@Getter
public class SummonHealthSkill extends BaseSkillManager {

    private ArrayList<String> littleNpcs = new ArrayList<>();
    private int lifeTime = -1;

    public SummonHealthSkill(String name) {
        super(name);
    }

    @Override
    protected void privateDisplay(Entity... entities) {
        if (entities.length == 0 || !(entities[0] instanceof LittleNpc)) {
            return;
        }
        for (String littleNpcName : littleNpcs) {
            MonsterConfig config = LittleMonsterMainClass.getInstance().monsters.get(littleNpcName);
            if (config != null) {
                IEntity littleNpc = config.spawn(getMaster().getEntity().add(Utils.rand() ? 1 : -1, 0, Utils.rand() ? 1 : -1), this.lifeTime);
                littleNpc.setDeathFollowMaster(true);
                littleNpc.setMasterHuman((EntityHuman) entities[0]);
            }

        }
    }
}
