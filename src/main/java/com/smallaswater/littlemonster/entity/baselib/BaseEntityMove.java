package com.smallaswater.littlemonster.entity.baselib;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.*;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.mob.EntityMob;
import cn.nukkit.entity.passive.EntityAnimal;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.BubbleParticle;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.route.RouteFinder;
import com.smallaswater.littlemonster.route.WalkerRouteFinder;
import com.smallaswater.littlemonster.threads.PluginMasterThreadPool;
import com.smallaswater.littlemonster.utils.Utils;
import nukkitcoders.mobplugin.entities.animal.WalkingAnimal;
import nukkitcoders.mobplugin.entities.monster.Monster;

import java.util.ArrayList;


/**
 * @author SmallasWater
 * Create on 2021/6/28 8:49
 * Package com.smallaswater.littlemonster.entity
 * AI算法 参考 @MobPlugin
 */
public abstract class BaseEntityMove extends BaseEntity {


    private static final double FLOW_MULTIPLIER = .1;

    protected RouteFinder route = new WalkerRouteFinder(this);

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
            if (target != null) {
                this.route.setDestination(target);
            }else {
                this.route.resetNodes();
            }
        }
    }

    /**
     * 等待时长
     * */
    public int waitTime = 0;

    /**
     * @return 是否寻找新目标
     */
    private boolean isNeedCheck(){
        return this.targetOption(this.followTarget) || //followTarget 不满足继续被锁定的要求
                this.target == null ||
                (config.isTargetPlayer() && !(followTarget instanceof Player)) || //主动锁定玩家，但现有目标不是玩家
                this.distance(this.target) < 1; //已移动到指定位置
    }

    /*private boolean checkFight(){
        return followTarget == null ||
                (config.isTargetPlayer() && !(followTarget instanceof Player))
                || (this instanceof LittleNpc && followTarget instanceof LittleNpc && Utils.canAttackNpc((LittleNpc) this,(LittleNpc) followTarget));
    }*/

    /**
     * 寻找目标并锁定
     */
    private void checkTarget() {
        if (this.isKnockback()) {
            return;
        }
        //if(this.isNeedCheck()) {
            //扫描附近实体
            if (this.passengers.isEmpty()) {
                //获取范围内可以攻击的生物
                ArrayList<EntityCreature> scanEntities = new ArrayList<>();
                for (Entity entity : Utils.getAroundPlayers(this,seeSize,true,true,true)) {
                    //忽略凋零头 盔甲架
                    if(entity.getNetworkId() == 19 || entity.getNetworkId() == 30){
                        continue;
                    }

                    if(entity instanceof EntityCreature && entity != this) {
                        if (this.canAttackEntity(entity)) {
                            scanEntities.add((EntityCreature) entity);
                            TargetWeighted targetWeighted = this.getTargetWeighted((EntityCreature) entity);
                            targetWeighted.setReason(TargetWeighted.REASON_AUTO_SCAN);
                        }
                    }
                }

                for (EntityCreature entity : this.targetWeightedMap.keySet()) {
                    if (!scanEntities.contains(entity) && this.targetOption(entity, this.distance(entity))) {
                        this.targetWeightedMap.remove(entity);
                    }else {
                        TargetWeighted targetWeighted = this.getTargetWeighted(entity);
                        //更新距离
                        targetWeighted.setDistance(this.distance(entity));
                    }
                }

                //entities.sort((p1, p2) -> Double.compare(this.distance(p1) - this.distance(p2), 0.0D));
                ArrayList<EntityCreature> entities = new ArrayList<>(this.targetWeightedMap.keySet());
                entities.sort((p1, p2) -> Double.compare(this.getTargetWeighted(p2).getFinalWeighted() - this.getTargetWeighted(p1).getFinalWeighted(), 0.0D));
                if (!entities.isEmpty()) {
                    EntityCreature entity = entities.get(0);
                    if (entity != this.getFollowTarget()) {
                        if(canAttackEntity(entity)) {
                            this.fightEntity(entity);
                        }
                    }
                }

//                CompletableFuture<>.supplyAsync(() -> {
//
//                }, PluginMasterThreadPool.ASYNC_EXECUTOR).thenAccept(isHasBlock::set);
                //要不这种操作放在异步
//                ArrayList<EntityCreature> entities = new ArrayList<>(this.targetWeightedMap.keySet());
//                entities.sort((p1, p2) -> Double.compare(this.getTargetWeighted(p2).getFinalWeighted() - this.getTargetWeighted(p1).getFinalWeighted(), 0.0D));
//                if (!entities.isEmpty()) {
//                    EntityCreature entity = entities.get(0);
//                    if (entity != this.getFollowTarget()) {
//                        this.fightEntity(entity);
//                    }
//                }
            }

            //获取寻路目标点
            if (this.route != null){
                if (this.route.isFinished() && this.route.hasArrivedNodeInaccurate(this)) {
                    this.target = this.route.next();
                    return;
                }else if (this.followTarget != null && !this.route.isSearching()){
                    this.route.setDestination(this.followTarget);
                }
            }

            //没有目标时
            if(this.getTargetVector() == null) {
                //随机移动
                if (this.config.isCanMove()) {
                    int x;
                    int z;
                    Vector3 nextTarget = null;
                    if (this.stayTime > 0) {
                        /*if (Utils.rand(1, 100) > 5) {
                            return;
                        }
                        x = Utils.rand(10, 30);
                        z = Utils.rand(10, 30);
                        nextTarget = this.add(Utils.rand() ? x : -x, Utils.rand(-20.0, 20.0) / 10, Utils.rand() ? z : -z);*/
                    } else if (Utils.rand(1, 10) == 1) {
                        x = Utils.rand(5, 20);
                        z = Utils.rand(5, 20);
                        this.stayTime = Utils.rand(60, 200);
                        nextTarget = this.add(Utils.rand() ? x : -x, /*Utils.rand(-20.0, 20.0) / 10*/0, Utils.rand() ? z : -z);
                        nextTarget.y+=5;
                        for (int i=0; i<10; i++) {
                            nextTarget.y--;
                            if (this.level.getBlock(nextTarget).canPassThrough() &&
                                    !this.level.getBlock(nextTarget.down()).canPassThrough()) {
                                break;
                            }
                        }
                    }/*else if (this.moveTime <= 0 || this.target == null) {
                        x = Utils.rand(10, 40);
                        z = Utils.rand(10, 40);
                        this.stayTime = 0;
                        this.moveTime = Utils.rand(20, 60);
                        nextTarget = this.add(Utils.rand() ? x : -x, 0, Utils.rand() ? z : -z);
                    }*/
                    if (nextTarget != null) {
                        this.target = nextTarget;
                        if (this.route != null) {
                            this.route.setDestination(nextTarget);
                        }
                    }
                }
            }

            //TODO 未知用途，需要进一步测试
            /*double distance = this.followTarget != null ? this.distance(followTarget) : 0;
            if (distance > seeSize || this.targetOption(followTarget, distance)) {
                this.setFollowTarget(null,false);
                return;
            }
            if(this.target == null) {
                this.stayTime = 0;
                this.moveTime = 0;
                if (this.passengers.isEmpty()) {
                    this.target = followTarget;
                }
            }*/
        /*}else {
            //如果有寻路节点 更新目标
            if (this.route != null && this.route.hasCurrentNode() && this.route.hasArrivedNode(this) && this.route.hasNext()) {
                this.target = this.route.next();
            }
        }*/
    }

    /**
     * 是否可以攻击目标实体 （主要为NPC配置文件规则限制）
     *
     * @param targetEntity 目标实体
     * @return 是否可以攻击
     */
    protected boolean canAttackEntity(Entity targetEntity) {
        if (this.targetOption(targetEntity, this.distance(targetEntity))) {
            return false;
        }
        if (this instanceof LittleNpc && targetEntity instanceof LittleNpc) {
            if (!Utils.canAttackNpc((LittleNpc) this, (LittleNpc) targetEntity,false)) {
                return true;
            }
        }
        if (!this.config.isActiveAttackEntity()) {
            if (!(targetEntity instanceof Player)) {
                return false;
            }
        }
        if (!this.config.isTargetPlayer() && targetEntity instanceof Player) {
            return false;
        }

        if(Server.getInstance().getPluginManager().getPlugin("MobPlugin") != null){
            if(!config.isAttackFriendEntity()) {
                if(targetEntity instanceof WalkingAnimal){
                    return false;
                }
            }
            if(!config.isAttackHostileEntity()) {
                if(targetEntity instanceof Monster){
                    return false;
                }
            }
        }
        if(!config.isAttackFriendEntity()){
            return !(targetEntity instanceof EntityAnimal);
        }
        if(!config.isAttackHostileEntity()) {
            return !(targetEntity instanceof EntityMob);
        }

        return true;
    }

    /**
     * 锁定生物
     * */
    private boolean fightEntity(EntityCreature entity) {
        /*EntityCreature creature;
        double distance;
        creature = entity;*/
        /*distance = this.distance(creature);
        if (distance > seeSize || this.targetOption(creature, distance)) {
            return false;
        }*/
        this.stayTime = 0;
        this.moveTime = 0;
        this.setFollowTarget(entity, true);
        //this.target = entity;
        /*this.followTarget = creature;
        if (this.route == null && this.passengers.isEmpty()) {
            this.target = creature;
        }
        canAttack = true;*/
        return true;
    }

    /**
     * 生物移除
     * */
    abstract public void onClose();

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.config == null) {
            this.close();
        }
        if (this.closed) {
            return false;
        } else if (!this.isAlive()) {
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
            //攻击目标实体
            if(target instanceof EntityCreature) {
                if(this.targetOption((EntityCreature) target, this.distance(target))){
                    this.setFollowTarget(null,false);
                    return true;
                }
                if(target instanceof Player) {
                    Player player = (Player) target;
                    if (target != this.followTarget || this.canAttack) {
                        this.attackEntity(player);
                    }
                }else {
                    if (this.canAttack) {
                        this.attackEntity((EntityCreature) target);
                    }
                }
            }else if (target != null && this.distance(target) < 0.8) {
                this.target = null;
                this.moveTime = 0;
            }

            return true;
        }
    }

    private Vector3 updateMove(int tickDiff) {
        if (this.isImmobile() || !this.isMovement()) {
            return null;
        }

        if (this.age % 10 == 0 && this.route != null && this.route.needSearching()) {
            //RouteFinderThreadPool.executeRouteFinderThread(new RouteFinderSearchTask(this.route));
        }

        if (this.isKnockback()) {
            this.move(this.motionX, this.motionY, this.motionZ);
            this.motionY -= this.getGravity();
            this.updateMovement();
            return null;
        }

        Vector3 before = this.target;
        PluginMasterThreadPool.ASYNC_EXECUTOR.submit(this::checkTarget);
//        this.checkTarget();
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

                //如果不停留 且未到达目标点
                if (this.stayTime <= 0 && this.distance(this.target) > ((double) this.getWidth() + 0.0D) / 2.0D + 0.05D) {
                    //计算移动方向
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
                if ((this.passengers.isEmpty()) && (this.stayTime <= 0 /*|| Utils.rand()*/)) {
                    //看向移动方向
                    if (this.followTarget == null) {
                        this.yaw = Math.toDegrees(-Math.atan2(x / diff, z / diff));
                        this.pitch = 0;
                    }
                }
            }
        }else {
            this.motionX = 0;
            this.motionZ = 0;
        }

        if (this.passengers.isEmpty() && this.stayTime <= 0) {
            this.seeFollowTarget();
        }

        x = this.motionX;
        z = this.motionZ;
        boolean isJump = this.checkJump(x, z);
        if (this.stayTime > 0) {
            this.stayTime -= tickDiff;
            this.move(0.0D, this.motionY, 0.0D);
        } else {
            Vector2 be = new Vector2(this.x + x, this.z + z);
            if(attactMode != 3 && attactMode != 2){
                waitTime = 0;
            }else if(followTarget == null || (this.distance(followTarget) > seeSize)){
                waitTime = 0;
            }else{
                waitTime++;
                if(waitTime >= 20 * 5){
                    waitTime = 0;
                    this.setFollowTarget(null,false);
                }
            }
            this.move(x, this.motionY, z);
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
        /*if (this.route != null && this.route.hasCurrentNode() && this.route.hasArrivedNode(this) && this.route.hasNext()) {
            this.target = this.route.next();
        }*/
        return this.followTarget != null ? this.followTarget : this.target;
    }

    protected void seeFollowTarget() {
        Vector3 target = null;
        if(!hasBlockInLine(this.followTarget)) {
            if (this.followTarget != null) {
                target = this.followTarget;
            }
        }else {
            target = this.target;
        }

        if (target != null) {
            boolean setPitch = false;
            if (target instanceof Entity) {
                target = target.add(0, ((Entity) target).getEyeHeight(), 0);
                setPitch = true;
            }
            double dx = this.x - target.x;
            double dy = (this.y + this.getEyeHeight()) - target.y;
            double dz = this.z - target.z;
            double yaw = Math.asin(dx / Math.sqrt(dx * dx + dz * dz)) / Math.PI * 180.0D;
            double pitch = Math.round(Math.asin(dy / Math.sqrt(dx * dx + dz * dz + dy * dy)) / Math.PI * 180.0D);
            if (dz > 0.0D) {
                yaw = -yaw + 180.0D;
            }
            this.yaw = yaw;
            this.pitch = setPitch ? pitch : 0;
        }
    }

    public RouteFinder getRoute() {
        return this.route;
    }

    public void setRoute(RouteFinder route) {
        this.route = route;
    }

}
