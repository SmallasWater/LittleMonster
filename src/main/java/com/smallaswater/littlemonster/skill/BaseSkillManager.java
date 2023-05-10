package com.smallaswater.littlemonster.skill;

import cn.nukkit.entity.Entity;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.skill.defaultskill.*;
import com.smallaswater.littlemonster.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author SmallasWater
 * Create on 2021/6/29 15:10
 * Package com.smallaswater.littlemonster.skill
 * 血量技能
 */
public abstract class BaseSkillManager implements Cloneable {

    public String name;

    public int mode;

    private LittleNpc master;

    private Number effect;

    @Setter
    public int health;

    @Getter
    @Setter
    private int probability;

    public BaseSkillManager(String name) {
        this.name = name;
    }

    private static final ConcurrentHashMap<String, BaseSkillManager> MANAGER_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    public static void initSkill() {
        register("Attribute", new AttributeHealthSkill("Attribute"));
        register("Fire", new FireHealthSkill("Fire"));
        register("Ice", new IceHealtthSkill("Ice"));
        register("KnockBack", new KnockBackHealthSkill("KnockBack"));
        register("Message", new MessageHealthSkill("Message"));
        register("Summon", new SummonHealthSkill("Summon"));
        register("Effect", new EffectHealthSkill("Effect"));
    }

    private static void register(String name, BaseSkillManager manager) {
        MANAGER_CONCURRENT_HASH_MAP.put(name, manager);
    }


    public static BaseSkillManager get(String name) {
        if (MANAGER_CONCURRENT_HASH_MAP.containsKey(name)) {
            return MANAGER_CONCURRENT_HASH_MAP.get(name).clone();
        }
        return null;
    }

    public void setEffect(Number effect) {
        this.effect = effect;
    }

    protected Number getEffect() {
        return effect;
    }

    public void setMaster(LittleNpc master) {
        this.master = master;
    }

    protected LittleNpc getMaster() {
        return master;
    }

    public void display(Entity... player) {
        if (this.probability >= Utils.rand(1, 100)) {
            this.privateDisplay(player);
        }
    }

    protected abstract void privateDisplay(Entity... entities);

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseSkillManager) {
            return ((BaseSkillManager) obj).getName().equals(getName())
                    && ((BaseSkillManager) obj).health == health;
        }
        return false;
    }

    @Override
    public BaseSkillManager clone() {
        try {
            return (BaseSkillManager) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "{name:" + name + "health:" + health + "}";
    }
}
