package com.smallaswater.littlemonster.entity;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.entity.EntityDeathEvent;
import com.smallaswater.littlemonster.config.MonsterConfig;

public interface IEntity {
    Entity getEntity();
    void setLiveTime(int time);
    void setCanBeSavedWithChunk(boolean is);
    void spawnToAll();

    void setSpawnPos(String name);
    String getSpawnPos();

    void setDeathFollowMaster(boolean b);

    void setMasterHuman(EntityHuman entity);
    MonsterConfig getConfig();

    boolean isVanillaEntity();

    void onDeath(EntityDeathEvent e);
}
