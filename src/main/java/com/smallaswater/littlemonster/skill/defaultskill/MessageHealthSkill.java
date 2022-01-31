package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import com.smallaswater.littlemonster.skill.BaseSkillManager;

/**
 * @author SmallasWater
 * Create on 2021/6/29 23:11
 * Package com.smallaswater.littlemonster.skill.defaultskill
 */
public class MessageHealthSkill extends BaseSkillManager {

    private String text;

    public MessageHealthSkill(String name) {
        super(name);
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void display(Entity... player) {
        for(Entity player1: player){
            if(player1 instanceof Player) {
                if (((Player) player1).isOnline()) {
                    switch (mode) {
                        case 0:
                            ((Player) player1).sendMessage(text);
                            break;
                        case 1:
                            ((Player) player1).sendTitle(text);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }
}
