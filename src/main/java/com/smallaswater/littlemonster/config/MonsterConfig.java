package com.smallaswater.littlemonster.config;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.PlayerSkinPacket;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.Config;
import com.smallaswater.littlemonster.LittleMasterMainClass;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.items.DeathCommand;
import com.smallaswater.littlemonster.items.DropItem;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import com.smallaswater.littlemonster.skill.defaultskill.*;
import com.smallaswater.littlemonster.utils.Utils;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author SmallasWater
 * Create on 2021/6/28 7:49
 * Package com.smallaswater.littlemaster.config
 */
@Data
public class MonsterConfig {

    private Config config;

    private String name;

    private String tag;

    private int damage = 2;

    private boolean unFightHeal = true;

    private int health = 20;

    private int delDamage = 0;

    private double size = 1;

    private Item item;

    private boolean konck;

    private int healTime;

    private String campName = "光明";

    private boolean canAttackSameCamp = false;

    private LinkedList<Effect> effects = new LinkedList<>();

    private LinkedList<DeathCommand>  deathCommand = new LinkedList<>();

    private LinkedList<DropItem> deathItem = new LinkedList<>();

    private boolean move;

    private int heal;

    /**攻击距离: 0.1
     攻击速度: 23
     攻击方式: 0
     移动速度: 1.0
     无敌时间: 3
     */
    private double attackDistance;

    private int attaceSpeed;

    private int attaceMode;

    private double moveSpeed;

    private int invincibleTime;

    /**
     * 主动锁定玩家
     */
    private boolean targetPlayer;

    private boolean passiveAttackEntity;

    /**
     * 主动攻击生物
     */
    private boolean activeAttackEntity;

    private int seeLine;

    private double knockBack = 0.8;

    private Item offhand;

    private ArrayList<BaseSkillManager> skillManagers = new ArrayList<>();

    private int area;

    private String skin;

    private boolean displayDamage;

    private boolean attackFriendEntity;

    private boolean attackHostileEntity;

    private boolean canMove;

    private ArrayList<Item> armor = new ArrayList<>();

    private ArrayList<String> damageCamp = new ArrayList<>();

    private ArrayList<String> toDamageCamp = new ArrayList<>();

    private MonsterConfig(String name){
        this.name = name;
    }

    public static MonsterConfig loadEntity(String name, Config config){
        MonsterConfig entity = new MonsterConfig(name);
        entity.setConfig(config);
        entity.setTag(config.getString("头部显示"));
        entity.setDamage(config.getInt("攻击力"));
        entity.setHealth(config.getInt("血量"));
        entity.setKnockBack(config.getDouble("击退距离",0.8));
        entity.setTargetPlayer(config.getBoolean("主动锁定玩家"));
        entity.setAttackHostileEntity(config.getBoolean("是否攻击敌对生物",true));
        entity.setAttackFriendEntity(config.getBoolean("是否攻击友好生物",false));
        entity.setActiveAttackEntity(config.getBoolean("是否主动攻击生物",true));
        entity.setPassiveAttackEntity(config.getBoolean("是否被动回击生物",true));
        entity.setMove(config.getBoolean("是否可移动",true));
        entity.setCanAttackSameCamp(config.getBoolean("是否攻击相同阵营",false));
        entity.setDelDamage(config.getInt("防御"));
        entity.setDamageCamp(new ArrayList<>(config.getStringList("攻击阵营")));
        entity.setToDamageCamp(new ArrayList<>(config.getStringList("回击阵营")));
        entity.setCampName(config.getString("阵营","光明"));
        entity.setHealTime(config.getInt("恢复间隔",20));
        entity.setUnFightHeal(config.getBoolean("是否仅脱战恢复"));
        entity.setSeeLine(config.getInt("视觉距离",15));
        entity.setHeal(config.getInt("恢复血量",5));
        entity.setDisplayDamage(config.getBoolean("显示伤害榜",true));
        entity.setArea(config.getInt("群体攻击范围",5));
        entity.setCanMove(config.getBoolean("未锁定时是否移动",true));
        entity.setKonck(config.getBoolean("是否可击退",false));
        entity.setSkin(config.getString("皮肤","粉蓝双瞳猫耳少女"));
        entity.setAttaceSpeed(config.getInt("攻击速度",23));
        entity.setAttaceMode(config.getInt("攻击方式",0));
        entity.setAttackDistance(config.getDouble("攻击距离",0.1));
        entity.setMoveSpeed(config.getDouble("移动速度",1.0));
        entity.setInvincibleTime(config.getInt("无敌时间",3));
        BaseSkillManager skillManager;
        Map skillConfig = (Map) config.get("技能");
        for(Object health: skillConfig.keySet()){

            List list = (List) skillConfig.get(health);
            for(Object o:list){
                if(o instanceof Map){
                    skillManager = fromSkill(health.toString(),(Map) o);
                    if(skillManager != null){
                        entity.addSkill(skillManager);
                    }
                }
            }
        }

        entity.setSize(config.getDouble("大小"));
        ArrayList<Item> armor = new ArrayList<>();
        entity.setItem(Item.fromString(config.getString("装饰.手持","267:0")));
        armor.add(Item.fromString(config.getString("装饰.头盔","0:0")));
        armor.add(Item.fromString(config.getString("装饰.胸甲","0:0")));
        armor.add(Item.fromString(config.getString("装饰.护腿","0:0")));
        armor.add(Item.fromString(config.getString("装饰.靴子","0:0")));
        entity.setArmor(armor);
        entity.setOffhand(Item.fromString(config.getString("装饰.副手","267:0")));
        List<String> effect = config.getStringList("药水效果");
        entity.setEffects(Utils.effectFromString(effect));
        List<Map> maps = config.getMapList("死亡掉落.cmd");
        LinkedList<DeathCommand> commands = new LinkedList<>();
        for(Map m:maps){
            commands.add(new DeathCommand(m));
        }
        entity.setDeathCommand(commands);
        List<Map> map =  config.getMapList("死亡掉落.item");
        LinkedList<DropItem> items = new LinkedList<>();
        for(Map map1:map){
            DropItem item = DropItem.toItem(map1.get("id").toString(),Integer.parseInt(map1.get("round").toString()));
            if(item != null){
                items.add(item);

            }
        }
        entity.setDeathItem(items);
        return entity;
    }

    public boolean isImmobile(){
        return !move;
    }

    public void addSkill(BaseSkillManager skillManager){
        skillManagers.add(skillManager);
    }

    public void set(String name,Object o){
        config.set(name, o);
    }

    public LittleNpc spawn(Position spawn, int time){
        Skin skin = new Skin();
        if(LittleMasterMainClass.loadSkins.containsKey(getSkin())){
            skin = LittleMasterMainClass.loadSkins.get(getSkin());
        }

        LittleNpc littleNpc = new LittleNpc(spawn.getChunk(),
                Entity.getDefaultNBT(spawn).
                        putCompound("Skin", new CompoundTag()
                .putByteArray("Data", skin.getSkinData().data)
                .putString("ModelId", skin.getSkinId())),this);
        npcSetting(littleNpc);
        if(time > 0){
            littleNpc.setLiveTime(time);
        }
        littleNpc.spawnToAll();
        return littleNpc;
    }

    public void spawn(Position spawn){
        this.spawn(spawn,-1);
    }

    public void npcSetting(LittleNpc littleNpc) {
        littleNpc.setNameTag(getTag()
                .replace("{名称}",littleNpc.name)
                .replace("{血量}",littleNpc.getHealth()+"")
                .replace("{最大血量}",littleNpc.getMaxHealth()+""));
        littleNpc.setConfig(this);
        littleNpc.speed = (float) getMoveSpeed();
        littleNpc.damage = getDamage();
        littleNpc.setHealth(getHealth());
        littleNpc.setMaxHealth(getHealth());
        littleNpc.setScale((float) getSize());
        littleNpc.distanceLine = getAttackDistance();
        littleNpc.seeSize = getSeeLine();
        littleNpc.attackSleepTime = getAttaceSpeed();
        littleNpc.attactMode = getAttaceMode();
        littleNpc.heal = getHeal();
        littleNpc.healSettingTime = getHealTime();
        littleNpc.setImmobile(isImmobile());
        Skin skin = new Skin();
        if(LittleMasterMainClass.loadSkins.containsKey(getSkin())){
            skin =  LittleMasterMainClass.loadSkins.get(getSkin());
        }
        PlayerSkinPacket data = new PlayerSkinPacket();
        data.skin = skin;
        data.newSkinName = skin.getSkinId();
        data.oldSkinName = littleNpc.getSkin().getSkinId();
        data.uuid = littleNpc.getUniqueId();
        littleNpc.getInventory().setItemInHand(item);
        littleNpc.getInventory().setArmorContents(armor.toArray(new Item[0]));
        littleNpc.getOffhandInventory().setItem(0,offhand);
        littleNpc.setSkin(skin);
        Server.getInstance().getOnlinePlayers().values().forEach(p -> p.dataPacket(data));
        littleNpc.setSkin(skin);
    }

    public void saveAll(){
        config.save();
    }

    private static BaseSkillManager fromSkill(String health,Map map){
        BaseSkillManager skillManager = null;
        if(map.containsKey("技能名")){
            Object effect = map.get("效果");
            skillManager = fromSkillByName(map.get("技能名").toString());
            if(skillManager == null){
                return null;
            }
            skillManager.health = Integer.parseInt(health);
            if(skillManager instanceof AttributeHealthSkill){
                if(((AttributeHealthSkill) skillManager).getAttributeType() == AttributeHealthSkill.AttributeType.SKIN){
                    if(LittleMasterMainClass.loadSkins.containsKey(effect.toString())){
                        Skin skin = LittleMasterMainClass.loadSkins.get(effect.toString());
                        ((AttributeHealthSkill) skillManager).setSkin(skin);
                    }else{
                        return null;
                    }
                }else{
                    skillManager.setEffect(Double.parseDouble(effect.toString()));
                }
            }else{
                if(skillManager instanceof EffectHealthSkill){
                    if(map.containsKey("药水")){
                        ((EffectHealthSkill) skillManager).setEffects(Utils.effectFromString(asStringList((List) effect)));
                    }

                }else if(skillManager instanceof KnockBackHealthSkill){
                    skillManager.setEffect(Double.parseDouble(effect.toString()));
                }else if(skillManager instanceof MessageHealthSkill){
                    if(map.containsKey("信息")){
                        skillManager.mode = Integer.parseInt(effect.toString());
                        ((MessageHealthSkill) skillManager).setText(map.get("信息").toString());
                    }
                }else if(skillManager instanceof SummonHealthSkill){
                    ArrayList<String> npcs = new ArrayList<>();
                    for(Object o: (List)effect){
                       npcs.add(o.toString());
                    }
                    ((SummonHealthSkill) skillManager).setLittleNpcs(npcs);
                }else{
                    skillManager.setEffect(Integer.parseInt(effect.toString()));
                }

            }
        }
        return skillManager;
    }

    private static List<String> asStringList(List list){
        ArrayList<String> strings = new ArrayList<>();
        for(Object o:list){
            strings.add(o.toString());
        }
        return strings;
    }

    private static BaseSkillManager fromSkillByName(String name){
        BaseSkillManager skill = null;
        int mode = 0;
        switch (name){
            case "@药水":
                skill = BaseSkillManager.get("Effect");
                break;
            case "@群体药水":
                mode = 1;
                skill = BaseSkillManager.get("Effect");
                break;
            case "@体型":
                skill = BaseSkillManager.get("Attribute");
                if(skill != null){
                    ((AttributeHealthSkill) skill).setAttributeType(AttributeHealthSkill.AttributeType.SCALE);
                }
              break;
            case "@伤害":
                skill = BaseSkillManager.get("Attribute");
                if(skill != null){
                    ((AttributeHealthSkill) skill).setAttributeType(AttributeHealthSkill.AttributeType.DAMAGE);
                }

                break;
            case "@攻速":
                skill = BaseSkillManager.get("Attribute");
                if(skill != null){
                    ((AttributeHealthSkill) skill).setAttributeType(AttributeHealthSkill.AttributeType.ATTACK_SPEED);
                }
                break;
            case "@皮肤":
                skill = BaseSkillManager.get("Attribute");
                if(skill != null){
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
                skill =  BaseSkillManager.get("Ice");
                break;
            case "@冰冻":
                skill =  BaseSkillManager.get("Ice");
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
            default:break;
        }
        if(skill != null) {
            skill.mode = mode;
        }
        return skill;
    }

    public void resetEntity(){
        for(LittleNpc entity: Utils.getEntitys(getName())){
            entity.setConfig(this);
            npcSetting(entity);
        }
    }

    @Override
    public String toString(){
        return "name: "+name+" ->"+"camp: "+getCampName();
    }
}
