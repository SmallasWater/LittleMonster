package com.smallaswater.littlemonster.entity.baselib;


import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import com.smallaswater.littlemonster.LittleMasterMainClass;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.skill.BaseSkillManager;

import java.util.ArrayList;


/**
 * @author SmallasWater
 * Create on 2021/6/28 7:57
 * Package com.smallaswater.littlemonster.entity
 */
public abstract class BaseEntity extends EntityHuman {

    protected float moveMultiplier = 1.0f;

    protected int healTime = 0;
    //停留
    int stayTime = 0;

    int moveTime = 0;
    //目标
    Vector3 target = null;
    //锁定生物
    Entity followTarget = null;

//    protected boolean disPlayAnim = false;

    private boolean movement = true;

    protected ArrayList<BaseSkillManager> skillManagers = new ArrayList<>();

    private boolean friendly = false;

    protected int attackDelay = 0;

    protected int damageDelay = 0;

    protected MonsterConfig config;

    boolean canAttack = true;

    //开发接口
    //攻击方式
    public int attactMode = 0;
    //攻击距离
    public double distanceLine = 0.1;
    //攻击速度
    public int attackSleepTime = 23;
    //伤害
    public double damage = 2;
    //移动速度
    public float speed = 1.0f;

    public int seeSize = 20;

    BaseEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

    }

    @Override
    protected void initEntity() {
        super.initEntity();
        if (this.namedTag.contains("Movement")) {
            this.setMovement(this.namedTag.getBoolean("Movement"));
        }

        if (this.namedTag.contains("Age")) {
            this.age = this.namedTag.getShort("Age");
        }
    }

    public boolean hasNoTarget(){
        return getFollowTarget() == null  || (getFollowTarget() != null && targetOption(getFollowTarget(),distance(getFollowTarget())));
    }

    public MonsterConfig getConfig() {
        return config == null ? config = LittleMasterMainClass.getMasterMainClass().monsters.get(getName()):config;
    }

    public int getAttackSleepTime() {
        return attackSleepTime;
    }

    public abstract int getKillExperience();

    public boolean isFriendly() {
        return this.friendly;
    }

    public boolean isMovement() {
        return this.movement;
    }

    public boolean isKnockback() {
        return this.attackTime > 0;
    }

    public void setFriendly(boolean bool) {
        this.friendly = bool;
    }

    public void setMovement(boolean value) {
        this.movement = value;
    }

    public double getSpeed() {
        return speed;
    }

    public Vector3 getTarget() {
        return this.target;
    }

    public void setTarget(Vector3 vec) {
        this.target = vec;
    }

    public Entity getFollowTarget() {
        return this.followTarget != null ? this.followTarget : (this.target instanceof EntityCreature ? (EntityCreature)this.target : null);
    }

    public void setFollowTarget(Entity target) {
        this.followTarget = target;
        this.moveTime = 0;
        this.stayTime = 0;
        this.target = null;
    }

    protected boolean isPlayerTarget(Player player){
        return !player.closed  && player.isAlive() && (player.isSurvival() || player.isAdventure()) ;
    }

    public boolean targetOption(Entity creature, double distance) {
        if (creature instanceof Player) {
            Player player = (Player)creature;
            return player.closed || !player.spawned || !player.isAlive() || (!player.isSurvival() && !player.isAdventure()) || distance > seeSize ||
                    !player.getLevel().getFolderName().equalsIgnoreCase(getLevel().getFolderName());
        }else{
            return creature.closed || !creature.isAlive() || !creature.getLevel().getFolderName().equalsIgnoreCase(getLevel().getFolderName());
        }

    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        super.entityBaseTick(tickDiff);
        if (!this.isAlive()) {
            this.close();
        }

        if (this instanceof LittleNpc && this.attackDelay < 1000) {
            ++this.attackDelay;
        }
        if (this instanceof LittleNpc && this.damageDelay < 1000) {
            ++this.damageDelay;
        }
        if (this instanceof LittleNpc && this.healTime < 1000) {
            ++this.healTime;
        }
        onUpdata();
        return true;
    }

    public abstract void onUpdata();

    public abstract void onAttack(EntityDamageEvent entity);

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.isKnockback() && source instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent)source).getDamager() instanceof Player) {
            return false;
        } else if (this.fireProof && (source.getCause()
                == EntityDamageEvent.DamageCause.FIRE || source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || source.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            return false;
        } else {
            if (source instanceof EntityDamageByEntityEvent) {
                ((EntityDamageByEntityEvent)source).setKnockBack(0.3F);
                onAttack(source);
            }
            this.target = null;
            this.stayTime = 0;
            super.attack(source);
            return true;
        }
    }

    public void setConfig(MonsterConfig config) {
        this.config = config;
    }

    /**攻击生物
     * @param player 生物
     * */
    abstract public void attackEntity(EntityCreature player);

    /**
     * 生物伤害
     * @return 伤害值
     * */
    abstract public float getDamage();

}
