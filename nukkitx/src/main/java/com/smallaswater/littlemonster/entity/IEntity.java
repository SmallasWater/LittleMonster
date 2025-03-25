package com.smallaswater.littlemonster.entity;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.entity.EntityDeathEvent;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.skill.BaseSkillManager;

import java.util.ArrayList;

public interface IEntity {

    Entity getEntity();

    void setLiveTime(int time);

    void spawnToAll();

    void setSpawnPos(String name);

    /**
     * 设置攻击模式
     *
     * @param mode 攻击模式
     */
    void setAttackMode(int mode);

    void setAttackDamage(int damage);

    void setEntityAttackSpeed(int speed);

    String getSpawnPos();

    void setDeathFollowMaster(boolean b);

    void setMasterHuman(EntityHuman entity);

    MonsterConfig getConfig();

    boolean isVanillaEntity();

    void onDeath(EntityDeathEvent e);

    void setSkinByIEntity(Skin skin);

    default void loadSkill() {
        BaseSkillManager baseSkillManager;
        for (BaseSkillManager skillManager : getConfig().getSkillManagers()) {
            baseSkillManager = skillManager.clone();
            baseSkillManager.setMaster(this);
            getSkillManagers().add(baseSkillManager);
        }
    }

    ArrayList<BaseSkillManager> getSkillManagers();
}
