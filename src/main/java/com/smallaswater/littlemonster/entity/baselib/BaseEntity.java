package com.smallaswater.littlemonster.entity.baselib;


import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.MoveEntityAbsolutePacket;
import cn.nukkit.network.protocol.SetEntityMotionPacket;
import com.smallaswater.littlemonster.LittleMasterMainClass;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import com.smallaswater.littlemonster.threads.PluginMasterThreadPool;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


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
            return true; //不能攻击自己
        }
        if (creature instanceof Player) {
            return !this.isPlayerTarget((Player) creature);
        }else{
            return creature.closed || !creature.isAlive() || !creature.getLevel().getFolderName().equalsIgnoreCase(getLevel().getFolderName()) || distance > seeSize;
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

    //判断中间是否有方块
    public boolean hasBlockInLine(Entity target){
        if(target != null) {
            //TODO 尝试异步调度获取
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                Block targetBlock = BaseEntity.this.getTargetBlock((int) BaseEntity.this.distance(target));
                if (targetBlock != null) {
                    return targetBlock.getId() != 0;
                }
                return false;
            }, PluginMasterThreadPool.EXECUTOR);
            future.thenAccept(e -> isHasBlock = e);
//            this.getTargetBlock()
//            //TODO 找到卡服原因
//            Block targetBlock = this.getLineOfSight((int) this.distance(target),1)[0];
//            if (targetBlock != null) {
//                return targetBlock.getId() != 0;
//            }
//            return false;
        }
        return isHasBlock;
    }

    private boolean isHasBlock = false;

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
}
