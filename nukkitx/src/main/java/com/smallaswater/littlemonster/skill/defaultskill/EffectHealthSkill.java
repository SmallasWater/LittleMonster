package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.entity.Entity;
import cn.nukkit.potion.Effect;
import com.smallaswater.littlemonster.skill.BaseSkillAreaManager;
import com.smallaswater.littlemonster.skill.BaseSkillManager;

import java.util.LinkedList;

/**
 * @author SmallasWater
 * Create on 2021/6/30 20:12
 * Package com.smallaswater.littlemonster.skill.defaultskill
 */
public class EffectHealthSkill extends BaseSkillManager implements BaseSkillAreaManager {

    private LinkedList<Effect> effects = new LinkedList<>();

    public void setEffects(LinkedList<Effect> effects) {
        this.effects = effects;
    }

    public EffectHealthSkill(String name) {
        super(name);
    }

    @Override
    protected void privateDisplay(Entity... entities) {
        for (Entity entity : entities) {
            for (Effect e : effects) {
                entity.addEffect(e);
            }
        }

    }
}
