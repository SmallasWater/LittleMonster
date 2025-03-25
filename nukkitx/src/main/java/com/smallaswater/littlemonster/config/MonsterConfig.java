package com.smallaswater.littlemonster.config;

import cn.lanink.gamecore.utils.NukkitTypeUtils;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.custom.EntityDefinition;
import cn.nukkit.entity.custom.EntityManager;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.Config;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.common.EntityTool;
import com.smallaswater.littlemonster.entity.IEntity;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.entity.LittleNpcCustomEntity;
import com.smallaswater.littlemonster.entity.vanilla.VanillaNPC;
import com.smallaswater.littlemonster.entity.vanilla.VanillaNPCCustomEntity;
import com.smallaswater.littlemonster.entity.vanilla.mob.EntitySlime;
import com.smallaswater.littlemonster.items.DeathCommand;
import com.smallaswater.littlemonster.items.DropItem;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import com.smallaswater.littlemonster.skill.defaultskill.*;
import com.smallaswater.littlemonster.utils.Utils;
import lombok.Data;

import java.util.*;

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

    private boolean knock;

    private int healTime;

    private String campName = "光明";

    private boolean canAttackSameCamp = false;

    private LinkedList<Effect> effects = new LinkedList<>();

    private LinkedList<DeathCommand> deathCommand = new LinkedList<>();

    private LinkedList<DropItem> deathItem = new LinkedList<>();

    private boolean move;

    private int heal;

    /**
     * 攻击距离
     */
    private double attackDistance;

    /**
     * 攻击速度
     */
    private int attackSpeed;

    /**
     * 攻击方式
     */
    private int attackMode;

    /**
     * 移动速度
     */
    private double moveSpeed;

    /**
     * 无敌时间
     */
    private int invincibleTime;

    /**
     * 主动锁定玩家
     */
    private boolean targetPlayer;

    /**
     * 被动回击生物
     */
    private boolean passiveAttackEntity;

    /**
     * 主动攻击生物
     */
    private boolean activeAttackEntity;

    private int seeLine;

    private double knockBack = 0.5;

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

    // 掉落经验
    private ArrayList<Integer> dropExp = new ArrayList<>();

    // 显示boss血条
    private boolean showBossBar = false;
    // 定义网络ID
    private int networkId;

    // 自定义实体相关设置
    private boolean enableCustomEntity;
    private EntityDefinition customEntityDefinition;
    private int customEntitySkinId;

    /**
     * 自动实体定义缓存
     */
    public static final HashMap<String, EntityDefinition> CUSTOM_ENTITY_DEFINITIONS = new HashMap<>();

    private MonsterConfig(String name) {
        this.name = name;
    }

    public static MonsterConfig loadEntity(String name, Config config) {
        try {
            MonsterConfig monsterConfig = new MonsterConfig(name);
            monsterConfig.setConfig(config);
            monsterConfig.setTag(config.getString("头部显示"));
            monsterConfig.setDamage(config.getInt("攻击力"));
            monsterConfig.setHealth(config.getInt("血量"));
            monsterConfig.setKnockBack(config.getDouble("击退距离", 0.5));
            monsterConfig.setTargetPlayer(config.getBoolean("主动锁定玩家"));
            monsterConfig.setAttackHostileEntity(config.getBoolean("是否攻击敌对生物", true));
            monsterConfig.setAttackFriendEntity(config.getBoolean("是否攻击友好生物", false));
            monsterConfig.setActiveAttackEntity(config.getBoolean("是否主动攻击生物", true));
            monsterConfig.setPassiveAttackEntity(config.getBoolean("是否被动回击生物", true));
            monsterConfig.setMove(config.getBoolean("是否可移动", true));
            monsterConfig.setCanAttackSameCamp(config.getBoolean("是否攻击相同阵营", false));
            monsterConfig.setDelDamage(config.getInt("防御"));
            monsterConfig.setDamageCamp(new ArrayList<>(config.getStringList("攻击阵营")));
            monsterConfig.setToDamageCamp(new ArrayList<>(config.getStringList("回击阵营")));
            monsterConfig.setCampName(config.getString("阵营", "光明"));
            monsterConfig.setHealTime(config.getInt("恢复间隔", 20));
            monsterConfig.setUnFightHeal(config.getBoolean("是否仅脱战恢复"));
            monsterConfig.setSeeLine(config.getInt("视觉距离", 15));
            monsterConfig.setHeal(config.getInt("恢复血量", 5));
            monsterConfig.setDisplayDamage(config.getBoolean("显示伤害榜", true));
            monsterConfig.setArea(config.getInt("群体攻击范围", 5));
            monsterConfig.setCanMove(config.getBoolean("未锁定时是否移动", true));
            monsterConfig.setKnock(config.getBoolean("是否可击退", false));
            monsterConfig.setSkin(config.getString("皮肤", "粉蓝双瞳猫耳少女"));
            monsterConfig.setAttackSpeed(config.getInt("攻击速度", 23));
            monsterConfig.setAttackMode(config.getInt("攻击方式", 0));
            monsterConfig.setAttackDistance(config.getDouble("攻击距离", 0.1));
            monsterConfig.setMoveSpeed(config.getDouble("移动速度", 1.0));
            monsterConfig.setInvincibleTime(config.getInt("无敌时间", 3));

            monsterConfig.setDropExp(new ArrayList<>(config.getIntegerList("掉落经验")));
            monsterConfig.setNetworkId(config.getInt("实体NetworkId", -1));
            monsterConfig.setShowBossBar(config.getBoolean("BOSS血条", false));

            if (config.getBoolean("CustomEntity.enable")) {
                if (NukkitTypeUtils.getNukkitType() == NukkitTypeUtils.NukkitType.MOT) {
                    monsterConfig.setEnableCustomEntity(true);
                    String identifier = config.getString("CustomEntity.identifier");
                    if(config.getInt("实体NetworkId") >= 10 && !CUSTOM_ENTITY_DEFINITIONS.containsKey(identifier)){
                        EntityDefinition entityDefinition = EntityDefinition.builder()
                                .identifier(identifier)
                                .spawnEgg(false)
                                .implementation(VanillaNPCCustomEntity.class)
                                .build();
                        EntityManager.get().registerDefinition(entityDefinition);
                        CUSTOM_ENTITY_DEFINITIONS.put(identifier, entityDefinition);
                    } else if (!CUSTOM_ENTITY_DEFINITIONS.containsKey(identifier)) {
                        EntityDefinition entityDefinition = EntityDefinition.builder()
                                .identifier(identifier)
                                .spawnEgg(false)
                                .implementation(LittleNpcCustomEntity.class)
                                .build();
                        EntityManager.get().registerDefinition(entityDefinition);
                        CUSTOM_ENTITY_DEFINITIONS.put(identifier, entityDefinition);
                    }
                    monsterConfig.setCustomEntityDefinition(CUSTOM_ENTITY_DEFINITIONS.get(identifier));
                    monsterConfig.setCustomEntitySkinId(config.getInt("CustomEntity.skinId"));
                } else {
                    LittleMonsterMainClass.getInstance().getLogger().warning(
                            "实体 " + name + " 启用自定义实体失败！当前版本仅支持 " + NukkitTypeUtils.NukkitType.MOT.getShowName() + " 核心！"
                    );
                }
            }

            HashMap<String, List<Object>> skillConfig = config.get("技能", new HashMap<>());
            for (String health : skillConfig.keySet()) {
                List<Object> list = skillConfig.get(health);
                for (Object o : list) {
                    if (o instanceof HashMap) {
                        BaseSkillManager skillManager = fromSkill(health, (HashMap<String, Object>) o);
                        if (skillManager != null) {
                            monsterConfig.addSkill(skillManager);
                        }
                    }
                }
            }

            monsterConfig.setSize(config.getDouble("大小"));

            monsterConfig.setItem(Item.fromString(config.getString("装饰.手持", "267:0")));
            ArrayList<Item> armor = new ArrayList<>();
            armor.add(Item.fromString(config.getString("装饰.头盔", "minecraft:air")));
            armor.add(Item.fromString(config.getString("装饰.胸甲", "minecraft:air")));
            armor.add(Item.fromString(config.getString("装饰.护腿", "minecraft:air")));
            armor.add(Item.fromString(config.getString("装饰.靴子", "minecraft:air")));
            monsterConfig.setArmor(armor);
            monsterConfig.setOffhand(Item.fromString(config.getString("装饰.副手", "267:0")));

            monsterConfig.setEffects(Utils.effectFromString(config.getStringList("药水效果")));

            LinkedList<DeathCommand> commands = new LinkedList<>();
            for (Map map : config.getMapList("死亡掉落.cmd")) {
                commands.add(new DeathCommand(map));
            }
            monsterConfig.setDeathCommand(commands);

            LinkedList<DropItem> items = new LinkedList<>();
            for (Map map : config.getMapList("死亡掉落.item")) {
                DropItem item = DropItem.toItem(map.get("id").toString(), Integer.parseInt(map.get("round").toString()));
                if (item != null) {
                    items.add(item);
                }
            }
            monsterConfig.setDeathItem(items);

            return monsterConfig;
        } catch (Exception e) {
            LittleMonsterMainClass.getInstance().getLogger().error("加载怪物" + name + "配置文件错误！", e);
            return null;
        }
    }

    public boolean isImmobile() {
        return !move;
    }

    public void addSkill(BaseSkillManager skillManager) {
        skillManagers.add(skillManager);
    }

    public void set(String name, Object o) {
        config.set(name, o);
    }

    public IEntity spawn(Position spawn, int time) {
        Skin skin = LittleMonsterMainClass.loadSkins.getOrDefault(getSkin(), new Skin());

        CompoundTag nbt = Entity.getDefaultNBT(spawn).
                putCompound("Skin", new CompoundTag()
                        .putByteArray("Data", skin.getSkinData().data)
                        .putString("ModelId", skin.getSkinId()));

        IEntity littleNpc;
        if (this.enableCustomEntity) {
            if(this.networkId >= 10 ){
                if(this.networkId == 37){
                    littleNpc = new EntitySlime(spawn.getChunk(), Entity.getDefaultNBT(spawn), this);
                }else{
                    littleNpc = new VanillaNPCCustomEntity(spawn.getChunk(), nbt, this,false);
                }
                this.vanillaSetting((VanillaNPC) littleNpc);
            } else {
                littleNpc = new LittleNpcCustomEntity(spawn.getChunk(), nbt, this);
                this.npcSetting((LittleNpc) littleNpc);
            }
        } else if (this.networkId != -1) {
            if (this.networkId == 37) {
                littleNpc = new EntitySlime(spawn.getChunk(), Entity.getDefaultNBT(spawn), this);
            } else {
                littleNpc = new VanillaNPC(spawn.getChunk(), Entity.getDefaultNBT(spawn), this, false);
            }
            this.vanillaSetting((VanillaNPC) littleNpc);
        } else {
            littleNpc = new LittleNpc(spawn.getChunk(), nbt, this);
            this.npcSetting((LittleNpc) littleNpc);
        }

        if (time > 0) {
            littleNpc.setLiveTime(time);
        }
        EntityTool.setEntityCanBeSavedWithChunk(littleNpc.getEntity(), false);
        littleNpc.spawnToAll();
        return littleNpc;
    }

    public IEntity spawn(Position spawn) {
        return this.spawn(spawn, -1);
    }

    public void vanillaSetting(VanillaNPC vanillaNpc) {
        vanillaNpc.setNameTag(getTag()
                .replace("{名称}", vanillaNpc.getName())
                .replace("{血量}", vanillaNpc.getHealth() + "")
                .replace("{最大血量}", vanillaNpc.getMaxHealth() + ""));
        vanillaNpc.speed = (float) getMoveSpeed() * 10;
        vanillaNpc.setAttackMode(getAttackMode());
        vanillaNpc.setAttackDamage(getDamage());
        vanillaNpc.setScale((float) getSize());
        vanillaNpc.routeMax = getSeeLine();
        vanillaNpc.entityAttackSpeed = getAttackSpeed();
        vanillaNpc.setTool(item);
        vanillaNpc.setArmor(armor.toArray(new Item[0]));
        //vanillaNpc.heal = getHeal();
        //vanillaNpc.healSettingTime = getHealTime();
        vanillaNpc.setImmobile(isImmobile());
        //vanillaNpc.getInventory().setItemInHand(item);
        //vanillaNpc.getInventory().setArmorContents(armor.toArray(new Item[0]));
        //vanillaNpc.getOffhandInventory().setItem(0, offhand);
    }

    public void npcSetting(LittleNpc littleNpc) {
        littleNpc.setNameTag(getTag()
                .replace("{名称}", littleNpc.getName())
                .replace("{血量}", littleNpc.getHealth() + "")
                .replace("{最大血量}", littleNpc.getMaxHealth() + ""));
        littleNpc.speed = (float) getMoveSpeed();
        littleNpc.setAttackMode(getAttackMode());
        littleNpc.setAttackDamage(getDamage());
        littleNpc.setMaxHealth(getHealth());
        littleNpc.setHealth(getHealth());
        littleNpc.setScale((float) getSize());
        littleNpc.seeSize = getSeeLine();
        littleNpc.entityAttackSpeed = getAttackSpeed();
        littleNpc.heal = getHeal();
        littleNpc.healSettingTime = getHealTime();
        littleNpc.setImmobile(isImmobile());
        Skin skin = new Skin();
        if (LittleMonsterMainClass.loadSkins.containsKey(getSkin())) {
            skin = LittleMonsterMainClass.loadSkins.get(getSkin());
        }
        littleNpc.getInventory().setItemInHand(item);
        littleNpc.getInventory().setArmorContents(armor.toArray(new Item[0]));
        littleNpc.getOffhandInventory().setItem(0, offhand);
        littleNpc.setSkin(skin);
    }

    public void saveAll() {
        config.save();
    }

    private static BaseSkillManager fromSkill(String health, HashMap<String, Object> map) {
        BaseSkillManager skillManager = null;
        if (map.containsKey("技能名")) {
            skillManager = BaseSkillManager.fromSkillByName(map.get("技能名").toString());
            if (skillManager == null) {
                LittleMonsterMainClass.getInstance().getLogger().warning("技能名 " + map.get("技能名").toString() + " 不存在！");
                return null;
            }

            skillManager.setHealth(Integer.parseInt(health));
            skillManager.setProbability(Integer.parseInt(map.getOrDefault("概率", 100).toString()));

            Object effect = map.get("效果");
            if (skillManager instanceof AttributeHealthSkill) {
                if (((AttributeHealthSkill) skillManager).getAttributeType() == AttributeHealthSkill.AttributeType.SKIN) {
                    if (LittleMonsterMainClass.loadSkins.containsKey(effect.toString())) {
                        Skin skin = LittleMonsterMainClass.loadSkins.get(effect.toString());
                        ((AttributeHealthSkill) skillManager).setSkin(skin);
                    } else {
                        return null;
                    }
                } else {
                    skillManager.setEffect(Double.parseDouble(effect.toString()));
                }
            } else {
                if (skillManager instanceof EffectHealthSkill) {
                    if (map.containsKey("药水")) {
                        ((EffectHealthSkill) skillManager).setEffects(Utils.effectFromString(Utils.asStringList((List) effect)));
                    }
                } else if (skillManager instanceof KnockBackHealthSkill) {
                    skillManager.setEffect(Double.parseDouble(effect.toString()));
                } else if (skillManager instanceof MessageHealthSkill) {
                    if (map.containsKey("信息")) {
                        skillManager.mode = Integer.parseInt(effect.toString());
                        ((MessageHealthSkill) skillManager).setText(map.get("信息").toString());
                    }
                } else if (skillManager instanceof SummonHealthSkill) {
                    ArrayList<String> npcs = new ArrayList<>();
                    for (Object o : (List) effect) {
                        npcs.add(o.toString());
                    }
                    ((SummonHealthSkill) skillManager).setLittleNpcs(npcs);
                } else {
                    skillManager.setEffect(Integer.parseInt(effect.toString()));
                }

            }
        }
        return skillManager;
    }

    public void resetEntity() {
        for (LittleNpc entity : Utils.getEntitys(getName())) {
            this.npcSetting(entity);
        }
    }

    @Override
    public String toString() {
        return "name: " + name + " ->" + "camp: " + getCampName();
    }
}
