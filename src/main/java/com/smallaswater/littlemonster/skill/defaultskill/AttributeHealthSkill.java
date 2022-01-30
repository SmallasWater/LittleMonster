package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.network.protocol.PlayerSkinPacket;
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
    public void display(Entity... player) {
        if(getMaster() != null){
            switch (attributeType){
                case SKIN:
                    PlayerSkinPacket data = new PlayerSkinPacket();
                    data.skin = skin;
                    data.newSkinName = skin.getSkinId();
                    data.oldSkinName = getMaster().getSkin().getSkinId();
                    data.uuid = getMaster().getUniqueId();
                    getMaster().setSkin(skin);
                    Server.getInstance().getOnlinePlayers().values().forEach(p -> p.dataPacket(data));
                    break;
                case SCALE:
                    getMaster().setScale(getEffect().floatValue());
                    break;
                case DAMAGE:
                    getMaster().damage = getEffect().doubleValue();
                    break;
                case ATTACK_SPEED:
                    getMaster().attackSleepTime = getEffect().intValue();
                    break;
                default:break;
            }
        }


    }

    public enum AttributeType{
        /**
         * 攻速*/
        ATTACK_SPEED,
        /**
         * 伤害*/
        DAMAGE,
        /**
         * 皮肤*/
        SKIN,
        /**
         * 大小
         * */
        SCALE

    }
}
