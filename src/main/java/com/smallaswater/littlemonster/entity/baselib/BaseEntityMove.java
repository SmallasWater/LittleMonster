package com.smallaswater.littlemonster.entity.baselib;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.*;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.BubbleParticle;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.route.RouteFinder;
import com.smallaswater.littlemonster.utils.Utils;
import nukkitcoders.mobplugin.entities.animal.Animal;
import nukkitcoders.mobplugin.entities.monster.Monster;


/**
 * @author SmallasWater
 * Create on 2021/6/28 8:49
 * Package com.smallaswater.littlemonster.entity
 * AI算法 参考 @MobPlugin
 */
public abstract class BaseEntityMove extends BaseEntity {


    private static final double FLOW_MULTIPLIER = .1;

    protected RouteFinder route = null;

    public BaseEntityMove(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    private boolean checkJump(double dx, double dz) {
        if (this.motionY == this.getGravity() * 2) {
            int b = level.getBlockIdAt(NukkitMath.floorDouble(this.x), (int) this.y, NukkitMath.floorDouble(this.z));
            return b == BlockID.WATER || b == BlockID.STILL_WATER;
        } else  {
            int b = level.getBlockIdAt(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8), NukkitMath.floorDouble(this.z));
            if (b == BlockID.WATER || b == BlockID.STILL_WATER) {
                if (this.target == null) {
                    this.motionY = this.getGravity() * 2;
                }
                return true;
            }
        }

        if (!this.onGround || this.stayTime > 0) {
            return false;
        }

        Block that = this.getLevel().getBlock(new Vector3(NukkitMath.floorDouble(this.x + dx), (int) this.y, NukkitMath.floorDouble(this.z + dz)));
        Block block = that.getSide(this.getHorizontalFacing());
        Block down = block.down();
        if (!down.isSolid() && !block.isSolid() && !down.down().isSolid()) {
            this.stayTime = 10;
        } else if (!block.canPassThrough() && block.up().canPassThrough() && that.up(2).canPassThrough()) {
            if (block instanceof BlockFence || block instanceof BlockFenceGate) {
                this.motionY = this.getGravity();
            } else if (this.motionY <= this.getGravity() * 4) {
                this.motionY = this.getGravity() * 4;
            } else if (block instanceof BlockStairs) {
                this.motionY = this.getGravity() * 4;
            } else if (this.motionY <= (this.getGravity() * 8)) {
                this.motionY = this.getGravity() * 8;
            } else {
                this.motionY += this.getGravity() * 0.25;
            }
            return true;
        }
        return false;
    }

    @Override
    public void setFollowTarget(Entity target) {
        this.setFollowTarget(target, true);
    }

    public void setFollowTarget(Entity target, boolean attack) {
        super.setFollowTarget(target);
        this.canAttack = attack;
        if (this.route != null) {
            this.route.setDestination(target);
        }
    }

    /**
     * 等待时长
     * */
    public int waitTime = 0;

    private boolean isCheck(){
        return (this.followTarget == null || this.followTarget.closed || !this.followTarget.isAlive()
                || this.targetOption(this.followTarget, this.distance(this.followTarget)) || this.target == null
                || (config.isTargetPlayer() && !(followTarget instanceof Player)));
    }

    private boolean checkFight(){
        return followTarget == null || (config.isTargetPlayer() && !(followTarget instanceof Player))
                || (followTarget instanceof LittleNpc && Utils.canAttackNpc((LittleNpc) this,(LittleNpc) followTarget));
    }


    /**
     * 寻找可以攻击的生物
     * */
    private void checkTarget() {
        if (!this.isKnockback()) {
            if(isCheck()) {
                double near = Integer.MAX_VALUE;
                double distance;
                if(checkFight()){
                    if (this.passengers.isEmpty()){
                        //获取范围内可以攻击的生物
                        for (Entity entity : Utils.getAroundPlayers(this,seeSize,true,true,true)) {
                            //忽略凋零头 盔甲架
                            if(entity.getNetworkId() == 19 || entity.getNetworkId() == 30){
                                continue;
                            }
                            if(entity instanceof EntityCreature) {
                                if (entity != this) {
                                    if (config.isTargetPlayer()) {
                                        if(entity instanceof Player) {
                                            getAttackChunk(near, entity);
                                            break;
                                        }else{
                                            if(followTarget == null) {
                                                getAttackChunk(near, entity);
                                            }
                                        }
                                    } else {
                                        if(entity instanceof Player){
                                            continue;
                                        }
                                        getAttackChunk(near, entity);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    //随便走..
                    if(config.isCanMove()) {
                        if (this.followTarget == null || this.followTarget.closed || !this.followTarget.isAlive() || this.targetOption(this.followTarget,
                                this.distance(this.followTarget)) || this.target == null) {
                            if (this.route.hasNext()) {
                                this.target = this.route.next();
                            }
                            int x = 0;
                            int z = 0;
                            if (this.stayTime > 0) {
                                if (Utils.rand(1, 100) > 5) {
                                    return;
                                }
                                x = Utils.rand(10, 30);
                                z = Utils.rand(10, 30);
                                this.target = this.add(Utils.rand() ? x : -x, Utils.rand(-20.0, 20.0) / 10, Utils.rand() ? z : -z);
                            } else if (Utils.rand(1, 100) == 1) {
                                x = Utils.rand(10, 30);
                                z = Utils.rand(10, 30);
                                this.stayTime = Utils.rand(100, 200);
                                this.target = this.add(Utils.rand() ? x : -x, Utils.rand(-20.0, 20.0) / 10, Utils.rand() ? z : -z);
                            } else if (this.moveTime <= 0 || this.target == null) {
                                x = Utils.rand(20, 100);
                                z = Utils.rand(20, 100);
                                this.stayTime = 0;
                                this.moveTime = Utils.rand(100, 200);
                                this.target = this.add(Utils.rand() ? x : -x, 0, Utils.rand() ? z : -z);
                            }
                            if (x != 0 && z != 0) {
                                this.route.setDestination(this.add(x, 0, z));
                            }
                        }
                    }
                }else{
                    distance = this.distance(followTarget);
                    if (distance > seeSize || this.targetOption(followTarget, distance)) {
                        setFollowTarget(null,false);
                        return;
                    }
                    if(this.target == null) {
                        this.stayTime = 0;
                        this.moveTime = 0;
                        if (this.passengers.isEmpty()) {
                            this.target = followTarget;
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查要锁定的生物
     * */
    private double getAttackChunk(double near, Entity entity) {
        if (!config.isActiveAttackEntity()) {
            if (!(entity instanceof Player)) {
                return near;
            }
        }
        if(Server.getInstance().getPluginManager().getPlugin("MobPlugin") != null){
            if(!config.isAttackFriendEntity()){
                if(entity instanceof Animal){
                    return near;
                }
            }
            if(!config.isAttackHostileEntity()){
                if(entity instanceof Monster){
                    return near;
                }
            }
        }
        near = getFightEntity(near, (EntityCreature) entity);
        return near;
    }

    /**
     * 锁定生物
     * */
    private double getFightEntity(double near, EntityCreature entity) {
        EntityCreature creature;
        double distance;
        creature = entity;
        distance = this.distance(creature);
        if (distance > seeSize || this.targetOption(creature, distance)) {
            return near;
        }
        near = distance;
        this.stayTime = 0;
        this.moveTime = 0;
        this.followTarget = creature;
        if (this.route == null && this.passengers.isEmpty()) {
            this.target = creature;
        }
        canAttack = true;
        return near;
    }

    /**
     * 生物移除
     * */
    abstract public void onClose();

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        } else if (!this.isAlive()) {
//            onClose();
            if (++this.deadTicks >= 23) {
                this.close();
                return false;
            } else {
                return true;
            }
        } else {
            int tickDiff = currentTick - this.lastUpdate;
            this.lastUpdate = currentTick;
            this.entityBaseTick(tickDiff);
            Vector3 target = this.updateMove(tickDiff);
            if(target instanceof EntityCreature){
                if(targetOption((EntityCreature) target,distance(target))){
                    setFollowTarget(null,false);
                    return true;
                }
                if(target instanceof Player){
                    if(isPlayerTarget((Player) target)) {
                        if (target != this.followTarget || this.canAttack) {
                            if(!targetOption((EntityCreature) target,distance(target))) {
                                this.attackEntity((Player) target);
                            }else{
                                setFollowTarget(null,false);
                            }
                        }
                    }
                }else{
//                    if (target != this.followTarget || this.canAttack) {
                        if (!targetOption((EntityCreature) target, distance(target))) {
                            this.attackEntity((EntityCreature) target);
                        }else{
                            setFollowTarget(null,false);
                        }
//                    }
                }
//
            }else if (target != null && Math.pow(this.x - target.x, 2.0D) + Math.pow(this.z - target.z, 2.0D) <= 1.0D) {
                this.moveTime = 0;
            }
            return true;
        }
    }

    private Vector3 updateMove(int tickDiff) {
        if (!this.isImmobile()) {
            if (!this.isMovement()) {
                return null;
            } else {
                if (this.age % 10 == 0 && this.route != null && this.route.isFinished()) {
                    //RouteFinderThreadPool.executeRouteFinderThread(new RouteFinderSearchTask(this.route));
                    if (this.route.hasNext()) {
                        this.target = this.route.next();
                    }
                }
                if (this.isKnockback()) {
                    this.move(this.motionX , this.motionY, this.motionZ);
                    this.motionY -= this.getGravity();
                    this.updateMovement();
                    return null;
                } else {
                    Vector3 before = this.target;
                    this.checkTarget();
                    double x;
                    double z;
                    if (this.target != null || before != this.target) {
                        if(this.target != null) {
                            x = this.target.x - this.x;
                            z = this.target.z - this.z;
                            double diff = Math.abs(x) + Math.abs(z);
                            if(diff <= 0){
                                diff = 0.1;
                            }
                            if (this.stayTime <= 0 && this.distance(this.target) > ((double) this.getWidth() + 0.0D) / 2.0D + 0.05D) {
                                if (this.isInsideOfWater()) {
                                    this.motionX = this.getSpeed() * 0.05D * (x / diff);
                                    this.motionZ = this.getSpeed() * 0.05D * (z / diff);
                                    this.level.addParticle(new BubbleParticle(this.add(Utils.rand(-2.0D, 2.0D), Utils.rand(-0.5D, 0.0D), Utils.rand(-2.0D, 2.0D))));
                                } else {
                                    this.motionX = this.getSpeed() * 0.15D * (x / diff);
                                    this.motionZ = this.getSpeed() * 0.15D * (z / diff);
                                }
                            } else {
                                this.motionX = 0.0D;
                                this.motionZ = 0.0D;
                            }
                            if ((this.passengers.isEmpty()) &&
                                    (this.stayTime <= 0 || Utils.rand())) {
                                this.yaw = Math.toDegrees(-Math.atan2(x / diff, z / diff));
                                if(!hasBlockInLine(followTarget)) {
                                    if (followTarget != null) {
                                        double dx = this.x - followTarget.x;
                                        double dy = (this.y + this.getEyeHeight()) - (followTarget.y + followTarget.getEyeHeight());
                                        double dz = this.z - followTarget.z;
                                        double yaw = Math.asin(dx / Math.sqrt(dx * dx + dz * dz)) / Math.PI * 180.0D;
                                        double pitch = Math.round(Math.asin(dy / Math.sqrt(dx * dx + dz * dz + dy * dy)) / Math.PI * 180.0D);
                                        if (dz > 0.0D) {
                                            yaw = -yaw + 180.0D;
                                        }
                                        this.yaw = yaw;
                                        this.pitch = pitch;
                                    } else {
                                        pitch = 0;
                                    }
                                }
                            }
                        }
                    }
                    x = this.motionX * (double)tickDiff;
                    z = this.motionZ * (double)tickDiff;
                    boolean isJump = this.checkJump(x, z);
                    if (this.stayTime > 0) {
                        this.stayTime -= tickDiff;
                        this.move(0.0D, this.motionY, 0.0D);
                    } else {
                        Vector2 be = new Vector2(this.x + x, this.z + z);
                        if(attactMode != 3 && attactMode != 2){
                            waitTime = 0;
                            this.move(x, this.motionY, z);
                        }else if(followTarget == null || (this.distance(followTarget) > seeSize)){
                            waitTime = 0;
                            this.move(x, this.motionY, z);
                        }else{
                            //判断直线距离中是否有方块阻挡
                            //if(hasBlockInLine(followTarget)){
                                this.move(x, this.motionY, z);
                            /*}else{
                                this.move(0.05, this.motionY, 0.05);
                            }*/
                            waitTime++;
                            if(waitTime >= 20 * 5){
                                waitTime = 0;
                                setFollowTarget(null,false);
                            }
                        }
                        Vector2 af = new Vector2(this.x, this.z);
                        if ((be.x != af.x || be.y != af.y) && !isJump) {
                            this.moveTime -= 90 * tickDiff;
                        }
                    }
                    if (!isJump) {
                        if (this.onGround) {
                            this.motionY = 0.0D;
                        } else if (this.motionY > (double)(-this.getGravity() * 4.0F)) {
                            if (!(this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int)(this.y + 0.8D), NukkitMath.floorDouble(this.z))) instanceof BlockLiquid)) {
                                this.motionY -= this.getGravity();
                            }
                        } else {
                            this.motionY -= this.getGravity() * (float)tickDiff;
                        }
                    }
                    this.updateMovement();
                    if (this.route != null && this.route.hasCurrentNode() && this.route.hasArrivedNode(this) && this.route.hasNext()) {
                        this.target = this.route.next();
                    }
                    return this.followTarget != null ? this.followTarget : this.target;
                }
            }
        } else {
            return null;
        }
    }

    public RouteFinder getRoute() {
        return this.route;
    }

    public void setRoute(RouteFinder route) {
        this.route = route;
    }

}
