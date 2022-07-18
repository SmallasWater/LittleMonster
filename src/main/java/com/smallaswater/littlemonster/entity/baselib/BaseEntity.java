package com.smallaswater.littlemonster.entity.baselib;


import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.MoveEntityAbsolutePacket;
import cn.nukkit.network.protocol.PlayerSkinPacket;
import cn.nukkit.network.protocol.SetEntityMotionPacket;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import com.smallaswater.littlemonster.threads.PluginMasterThreadPool;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author SmallasWater
 * Create on 2021/6/28 7:57
 * Package com.smallaswater.littlemonster.entity
 */
public abstract class BaseEntity extends EntityHuman {

    //如果主人死了 本体是否死亡
    public boolean isToDeath = false;

    protected EntityHuman masterHuman = null;

    protected float moveMultiplier = 1.0f;

    protected int healTime = 0;
    //停留
    int stayTime = 0;

    //目标
    Vector3 target = null;
    //锁定生物
    Entity followTarget = null;

    private boolean movement = true;

    protected ArrayList<BaseSkillManager> skillManagers = new ArrayList<>();

    protected int attackDelay = 0;

    protected int damageDelay = 0;

    protected MonsterConfig config;

    boolean canAttack = true;

    protected final ConcurrentHashMap<EntityCreature, TargetWeighted> targetWeightedMap = new ConcurrentHashMap<>();

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
        return config;
    }

    public int getAttackSleepTime() {
        return attackSleepTime;
    }

    public boolean isMovement() {
        return this.movement;
    }

    public boolean isKnockback() {
        return this.attackTime > 0;
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

    public Vector3 getTargetVector() {
        if (this.followTarget != null) {
            return this.followTarget;
        } else if (this.target != null) {
            return this.target;
        } else {
            return null;
        }
    }

    public void setMasterHuman(EntityHuman masterHuman) {
        this.masterHuman = masterHuman;
    }

    //准则 不会伤害主人
    public EntityHuman getMasterHuman() {
        return masterHuman;
    }

    public void setFollowTarget(Entity target) {
        this.followTarget = target;
        this.stayTime = 0;
        this.target = null;
    }

    /**
     * 检查玩家目标是否满足条件
     *
     * @param player 玩家
     * @return 是否满足继续跟踪的条件
     */
    protected boolean isPlayerTarget(Player player){
        return player.isOnline() && !player.closed && player.isAlive() &&
                (player.isSurvival() || player.isAdventure()) &&
                player.getLevel() == this.getLevel() &&
                this.distance(player) <= this.seeSize;
    }

    public boolean targetOption(Entity creature) {
        return this.targetOption(creature, (this.followTarget != null ? this.distance(this.followTarget) : this.seeSize + 1));
    }

    /**
     * 检查是否需要更换目标
     *
     * @param creature 目标
     * @param distance 距离
     * @return 是否需要更换目标
     */
    public boolean targetOption(Entity creature, double distance) {
        if (creature == null) {
            return true;
        }
        if (creature == this) {
            //不能攻击自己
            return true;
        }
        if(masterHuman != null && creature == masterHuman){
            //不能攻击主人
            return true;
        }
        if (creature instanceof Player) {
            return !this.isPlayerTarget((Player) creature);
        }else{
            return creature.closed || !creature.isAlive() || creature.getLevel() != this.getLevel() || distance > seeSize;
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

    public void setConfig(@NotNull MonsterConfig config) {
        this.config = Objects.requireNonNull(config);
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

    private final ReentrantLock hasBlockInLineLock = new ReentrantLock();
    private final AtomicBoolean isHasBlock = new AtomicBoolean();

    //判断中间是否有方块
    public boolean hasBlockInLine(Vector3 target){
        if(target != null && !this.hasBlockInLineLock.isLocked()) {
            try {
                CompletableFuture.supplyAsync(() -> {
                    hasBlockInLineLock.lock();
                    Block targetBlock = BaseEntity.this.getTargetBlock((int) BaseEntity.this.distance(target));
                    if (targetBlock != null) {
                        return !targetBlock.isTransparent();
                    }
                    return false;
                }, PluginMasterThreadPool.ASYNC_EXECUTOR).thenAccept(value -> {
                    this.isHasBlock.set(value);
                    hasBlockInLineLock.unlock();
                });
            } catch (Exception e) {
                this.hasBlockInLineLock.unlock();
            }
        }
        return this.isHasBlock.get();
    }

    @Override
    public boolean move(double dx, double dy, double dz) {
        if (dy < -10 || dy > 10) {
            return false;
        }

        double movX = dx * moveMultiplier;
        double movY = dy;
        double movZ = dz * moveMultiplier;

        AxisAlignedBB[] list = this.level.getCollisionCubes(this, this.boundingBox.addCoord(dx, dy, dz), false);
        for (AxisAlignedBB bb : list) {
            dx = bb.calculateXOffset(this.boundingBox, dx);
        }
        this.boundingBox.offset(dx, 0, 0);

        for (AxisAlignedBB bb : list) {
            dz = bb.calculateZOffset(this.boundingBox, dz);
        }
        this.boundingBox.offset(0, 0, dz);

        for (AxisAlignedBB bb : list) {
            dy = bb.calculateYOffset(this.boundingBox, dy);
        }
        this.boundingBox.offset(0, dy, 0);

        this.setComponents(this.x + dx, this.y + dy, this.z + dz);
        this.checkChunks();

        this.checkGroundState(movX, movY, movZ, dx, dy, dz);
        this.updateFallState(this.onGround);

        return true;
    }

    protected float getMountedYOffset() {
        return getHeight() * 0.75F;
    }


    public int nearbyDistanceMultiplier() {
        return 1;
    }

    private int airTicks = 0;

    @Override
    public int getAirTicks() {
        return this.airTicks;
    }

    @Override
    public void setAirTicks(int ticks) {
        this.airTicks = ticks;
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        MoveEntityAbsolutePacket pk = new MoveEntityAbsolutePacket();
        pk.eid = this.id;
        pk.x = (float) x;
        pk.y = (float) y;
        pk.z = (float) z;
        pk.yaw = (float) yaw;
        pk.headYaw = (float) headYaw;
        pk.pitch = (float) pitch;
        pk.onGround = this.onGround;
        for (Player p : this.hasSpawned.values()) {
            p.batchDataPacket(pk);
        }
    }

    @Override
    public void addMotion(double motionX, double motionY, double motionZ) {
        SetEntityMotionPacket pk = new SetEntityMotionPacket();
        pk.eid = this.id;
        pk.motionX = (float) motionX;
        pk.motionY = (float) motionY;
        pk.motionZ = (float) motionZ;
        for (Player p : this.hasSpawned.values()) {
            p.batchDataPacket(pk);
        }
    }

    @Override
    protected void checkGroundState(double movX, double movY, double movZ, double dx, double dy, double dz) {
        if (onGround && movX == 0 && movY == 0 && movZ == 0 && dx == 0 && dy == 0 && dz == 0) {
            return;
        }
        this.isCollidedVertically = movY != dy;
        this.isCollidedHorizontally = (movX != dx || movZ != dz);
        this.isCollided = (this.isCollidedHorizontally || this.isCollidedVertically);
        this.onGround = (movY != dy && movY < 0);
    }

    @Override
    public void resetFallDistance() {
        this.highestPosition = this.y;
    }

    @Override
    public boolean setMotion(Vector3 motion) {
        this.motionX = motion.x;
        this.motionY = motion.y;
        this.motionZ = motion.z;
        if (!this.justCreated) {
            this.updateMovement();
        }
        return true;
    }

    @Override
    public void setSkin(Skin skin) {
        PlayerSkinPacket packet = new PlayerSkinPacket();
        packet.skin = skin;
        packet.newSkinName = skin.getSkinId();
        packet.oldSkinName = this.getSkin() != null ? this.getSkin().getSkinId() : new Skin().getSkinId();
        packet.uuid = this.getUniqueId();
        for (Player player : this.getViewers().values()) {
            player.dataPacket(packet);
        }

        super.setSkin(skin);
    }


    public TargetWeighted getTargetWeighted(EntityCreature entity) {
        if (!this.targetWeightedMap.containsKey(entity)) {
            this.targetWeightedMap.put(entity, new TargetWeighted());
        }
        return this.targetWeightedMap.get(entity);
    }

    @Data
    public static class TargetWeighted {

        /**
         * 扫描附近实体
         */
        public static final double REASON_AUTO_SCAN = 10;
        /**
         * 被动反击
         */
        public static final double REASON_PASSIVE_ATTACK_ENTITY = 100.0;

        private int base = 1;
        private double reason = 0;
        private double causeDamage = 0;
        private double distance = 0;

        public void setReason(double reason) {
            this.setReason(reason, false);
        }

        public void setReason(double reason, boolean forcibly) {
            if (reason > this.reason || forcibly) {
                this.reason = reason;
            }
        }

        public double getFinalWeighted() {
            //TODO 计算公式
            return this.base + this.reason + (this.causeDamage * 1.2) - (this.distance * 0.5);
        }

    }

}
