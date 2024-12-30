package com.smallaswater.littlemonster.entity.vanilla.ai;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.LongEntityData;
import cn.nukkit.network.protocol.EntityEventPacket;

public class MeleeAttackExecutor {
    public static void playArmSwingAnimation(Entity entity) {
        //摆臂动作
        EntityEventPacket pk = new EntityEventPacket();
        pk.eid = entity.getId();
        pk.event = EntityEventPacket.ARM_SWING;
        Server.broadcastPacket(entity.getViewers().values(), pk);
    }
}
