package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.potion.Effect;
import com.smallaswater.littlemonster.skill.BaseSkillAreaManager;
import com.smallaswater.littlemonster.skill.BaseSkillManager;

import java.util.LinkedList;

/**
 * @author SmallasWater
 * Create on 2021/6/30 20:12
 * Package com.smallaswater.littlemonster.skill.defaultskill
 */
public class EffectHealthSkill extends BaseSkillManager  implements BaseSkillAreaManager {

    private LinkedList<Effect> effects = new LinkedList<>();

    public void setEffects(LinkedList<Effect> effects) {
        this.effects = effects;
    }

    public EffectHealthSkill(String name) {
        super(name);
    }

    @Override
    public void display(Entity... player) {
        for(Entity player1: player){
            for(Effect e:effects){
                player1.addEffect(e);
            }
        }

    }
}
