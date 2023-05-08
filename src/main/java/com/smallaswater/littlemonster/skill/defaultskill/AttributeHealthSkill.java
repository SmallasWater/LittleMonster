package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.Skin;
import com.smallaswater.littlemonster.skill.BaseSkillManager;

/**
 * @author SmallasWater
 * Create on 2021/6/29 20:08
 * Package com.smallaswater.littlemonster.skill.defaultskill
 */
public class AttributeHealthSkill extends BaseSkillManager {

    private Skin skin;

    private AttributeType attributeType;

    public AttributeHealthSkill(String name) {
        super(name);
    }

    public AttributeType getAttributeType() {
        return attributeType;
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    public void setAttributeType(AttributeType attributeType) {
        this.attributeType = attributeType;
    }

    @Override
    protected void privateDisplay(Entity... entities) {
        if (this.getMaster() != null) {
            switch (this.attributeType) {
                case SKIN:
                    this.getMaster().setSkin(this.skin);
                    break;
                case SCALE:
                    this.getMaster().setScale(getEffect().floatValue());
                    break;
                case DAMAGE:
                    this.getMaster().damage = getEffect().doubleValue();
                    break;
                case ATTACK_SPEED:
                    this.getMaster().attackSleepTime = getEffect().intValue();
                    break;
                default:
                    break;
            }
        }
    }

    public enum AttributeType {
        /**
         * 攻速
         */
        ATTACK_SPEED,
        /**
         * 伤害
         */
        DAMAGE,
        /**
         * 皮肤
         */
        SKIN,
        /**
         * 大小
         */
        SCALE

    }
}
