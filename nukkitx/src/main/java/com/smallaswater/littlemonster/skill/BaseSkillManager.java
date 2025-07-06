package com.smallaswater.littlemonster.skill;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.entity.IEntity;
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

    @Getter
    public String name;

    public int mode;

    @Setter
    private IEntity master;

    @Setter
    private Number effect;

    @Setter
    public int health;

    @Getter
    @Setter
    private int probability;

    @Getter
    @Setter
    private int delay = 0;

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
        register("SwitchAttackMode", new SwitchAttackModeSkill("SwitchAttackMode"));
        register("@命令", new CommandHealthSkill("@命令"));
    }

    /**
     * 注册技能
     *
     * @param name    技能名
     * @param manager 技能
     */
    private static void register(String name, BaseSkillManager manager) {
        MANAGER_CONCURRENT_HASH_MAP.put(name, manager);
    }

    /**
     * 获取技能
     *
     * @param name 技能名
     * @return 技能
     */
    public static BaseSkillManager get(String name) {
        if (MANAGER_CONCURRENT_HASH_MAP.containsKey(name)) {
            return MANAGER_CONCURRENT_HASH_MAP.get(name).clone();
        }
        return null;
    }

    /**
     * 通过技能名获取技能
     *
     * @param name 技能名
     * @return 技能
     */
    public static BaseSkillManager fromSkillByName(String name) {
        //TODO 为什么是注册英文名称，获取用中文？？？

        BaseSkillManager skill = null;
        int mode = 0;
        switch (name) {
            case "@药水":
                skill = BaseSkillManager.get("Effect");
                break;
            case "@群体药水":
                mode = 1;
                skill = BaseSkillManager.get("Effect");
                break;
            case "@体型":
                skill = BaseSkillManager.get("Attribute");
                if (skill != null) {
                    ((AttributeHealthSkill) skill).setAttributeType(AttributeHealthSkill.AttributeType.SCALE);
                }
                break;
            case "@伤害":
                skill = BaseSkillManager.get("Attribute");
                if (skill != null) {
                    ((AttributeHealthSkill) skill).setAttributeType(AttributeHealthSkill.AttributeType.DAMAGE);
                }

                break;
            case "@攻速":
                skill = BaseSkillManager.get("Attribute");
                if (skill != null) {
                    ((AttributeHealthSkill) skill).setAttributeType(AttributeHealthSkill.AttributeType.ATTACK_SPEED);
                }
                break;
            case "@皮肤":
                skill = BaseSkillManager.get("Attribute");
                if (skill != null) {
                    ((AttributeHealthSkill) skill).setAttributeType(AttributeHealthSkill.AttributeType.SKIN);
                }
                break;
            case "@群体引燃":
                mode = 1;
                skill = BaseSkillManager.get("Fire");
                break;
            case "@引燃":
                skill = BaseSkillManager.get("Fire");
                break;
            case "@群体冰冻":
                mode = 1;
                skill = BaseSkillManager.get("Ice");
                break;
            case "@冰冻":
                skill = BaseSkillManager.get("Ice");
                break;
            case "@范围击退":
                mode = 1;
                skill = BaseSkillManager.get("KnockBack");
                break;
            case "@击退":
                skill = BaseSkillManager.get("KnockBack");
                break;
            case "@信息":
                skill = BaseSkillManager.get("Message");
                break;
            case "@生成":
                skill = BaseSkillManager.get("Summon");
                break;
            case "@切换攻击模式":
                skill = BaseSkillManager.get("SwitchAttackMode");
            default:
                skill = BaseSkillManager.get(name);
                break;
        }
        if (skill != null) {
            skill.mode = mode;
        }
        return skill;
    }

    protected Number getEffect() {
        return effect;
    }

    protected IEntity getMaster() {
        return master;
    }

    public void display(Entity... player) {
        if (this.probability >= Utils.rand(1, 100)) {
            if (this.delay > 0) {
                Server.getInstance().getScheduler().scheduleDelayedTask(
                        LittleMonsterMainClass.getInstance(),
                        () -> this.privateDisplay(player),
                        this.delay
                );
            } else {
                this.privateDisplay(player);
            }
        }
    }

    protected abstract void privateDisplay(Entity... entities);

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
