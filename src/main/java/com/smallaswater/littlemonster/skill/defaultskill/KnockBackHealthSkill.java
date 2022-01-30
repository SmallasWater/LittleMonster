package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.entity.Entity;
import cn.nukkit.math.Vector3;
import com.smallaswater.littlemonster.skill.BaseSkillAreaManager;
import com.smallaswater.littlemonster.skill.BaseSkillManager;

/**
 * @author SmallasWater
 * Create on 2021/6/29 20:11
 * Package com.smallaswater.littlemonster.skill.defaultskill
 */
public class KnockBackHealthSkill extends BaseSkillManager  implements BaseSkillAreaManager {


    public KnockBackHealthSkill(String name) {
        super(name);
    }



    private void knockBack(Entity entity, double x, double z, double base) {
        double f = Math.sqrt(x * x + z * z);
        if (f > 0.0D) {
            f = 1.0D / f;
            Vector3 motion = new Vector3(entity.motionX, entity.motionY, entity.motionZ);
            motion.x /= 2.0D;
            motion.y /= 2.0D;
            motion.z /= 2.0D;
            motion.x += x * f * base;
            motion.y += base;
            motion.z += z * f * base;
            if (motion.y > base) {
                motion.y = base;
            }

            entity.setMotion(motion);
        }
    }
    @Override
    public void display(Entity... player) {
        for(Entity player1: player){
            knockBack(player1,(player1.x - getMaster().getLocation().x),(player1.z - getMaster().getLocation().z),getEffect().doubleValue());
        }

    }
}
