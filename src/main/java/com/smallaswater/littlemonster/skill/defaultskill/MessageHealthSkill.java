package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
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
    protected void privateDisplay(Entity... entities) {
        for (Entity entity : entities) {
            if (entity instanceof Player) {
                if (((Player) entity).isOnline()) {
                    switch (mode) {
                        case 0:
                            ((Player) entity).sendMessage(text);
                            break;
                        case 1:
                            ((Player) entity).sendTitle(text);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }
}
