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
import com.smallaswater.littlemonster.route.RouteFinder;
import com.smallaswater.littlemonster.utils.Utils;
import nukkitcoders.mobplugin.entities.animal.Animal;
import nukkitcoders.mobplugin.entities.monster.Monster;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;


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
        if (this.route != null && target != null) {
            this.route.setDestination(target);
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
        if(this.isNeedCheck()) {
            //扫描附近实体
            if (this.passengers.isEmpty()) {
                //获取范围内可以攻击的生物
                ArrayList<EntityCreature> entities = new ArrayList<>();
                for (Entity entity : Utils.getAroundPlayers(this,seeSize,true,true,true)) {
                    //忽略凋零头 盔甲架
                    if(entity.getNetworkId() == 19 || entity.getNetworkId() == 30){
                        continue;
                    }

                    if(entity instanceof EntityCreature && entity != this) {
                        if (this.canAttackEntity(entity)) {
                            entities.add((EntityCreature) entity);
                        }
                    }
                }

                //TODO 更智能的选取高优先级目标
                entities.sort((p1, p2) -> Double.compare(this.distance(p1) - this.distance(p2), 0.0D));
                if (!entities.isEmpty()) {
                    this.fightEntity(entities.get(0));
                }
                //TODO 删除调试信息
                Server.getInstance().getLogger().info("NPC:" + this.config.getName() + "目标" + entities);
            }

            //获取寻路目标点
            if (this.route != null && this.route.hasCurrentNode() && this.route.hasArrivedNode(this) && this.route.hasNext()) {
                this.target = this.route.next();
                return;
            }

            //随机移动
            if(this.config.isCanMove()) {
                /*if (this.followTarget == null || this.target == null) {
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
                }*/
            }

            //TODO 未知用途，需要进一步测试
            double distance = this.followTarget != null ? this.distance(followTarget) : 0;
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
            }
        }else {
            //如果有寻路节点 更新目标
            if (this.route != null && this.route.hasCurrentNode() && this.route.hasArrivedNode(this) && this.route.hasNext()) {
                this.target = this.route.next();
            }
        }
    }

    /**
     * 是否可以攻击目标实体 （主要为NPC配置文件规则限制）
     *
     * @param entity 目标实体
     * @return 是否可以攻击
     */
    private boolean canAttackEntity(Entity entity) {
        if (this.targetOption(entity)) {
            return false;
        }
        if (!this.config.isActiveAttackEntity()) {
            if (!(entity instanceof Player)) {
                return false;
            }
        }
        if (!this.config.isTargetPlayer() && entity instanceof Player) {
            return false;
        }

        if(Server.getInstance().getPluginManager().getPlugin("MobPlugin") != null){
            if(!config.isAttackFriendEntity()) {
                if(entity instanceof Animal){
                    return false;
                }
            }
            if(!config.isAttackHostileEntity()) {
                if(entity instanceof Monster){
                    return false;
                }
            }
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
            }else if (target != null && Math.pow(this.x - target.x, 2.0D) + Math.pow(this.z - target.z, 2.0D) <= 1.0D) {
                //已到达目标地点 可以在这里获取下一目标
                this.moveTime = 0;
            }

            return true;
        }
    }

    private Vector3 updateMove(int tickDiff) {
        if (this.isImmobile() || !this.isMovement()) {
            return null;
        }

        if (this.age % 10 == 0 && this.route != null && this.route.isFinished()) {
            //RouteFinderThreadPool.executeRouteFinderThread(new RouteFinderSearchTask(this.route));
            if (this.route.hasNext()) {
                this.target = this.route.next();
            }
        }

        if (this.isKnockback()) {
            this.move(this.motionX, this.motionY, this.motionZ);
            this.motionY -= this.getGravity();
            this.updateMovement();
            return null;
        }

        Block levelBlock = getLevelBlock();
        boolean inWater = levelBlock.getId() == 8 || levelBlock.getId() == 9;
        int downId = level.getBlockIdAt(getFloorX(), getFloorY() - 1, getFloorZ());
        if (inWater && (downId == 0 || downId == 8 || downId == 9 || downId == BlockID.LAVA || downId == BlockID.STILL_LAVA || downId == BlockID.SIGN_POST || downId == BlockID.WALL_SIGN)) onGround = false;
        if (downId == 0 || downId == BlockID.SIGN_POST || downId == BlockID.WALL_SIGN) onGround = false;
        if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive() && this.target!=null) {
            double x = this.target.x - this.x;
            double z = this.target.z - this.z;

            double diff = Math.abs(x) + Math.abs(z);
            if (!inWater && (this.stayTime > 0 || this.distance(this.followTarget) <= (this.getWidth()) / 2 + 0.05)) {
                this.motionX = 0;
                this.motionZ = 0;
            } else {
                if (levelBlock.getId() == 8) {
                    BlockWater blockWater = (BlockWater) levelBlock;
                    Vector3 flowVector = blockWater.getFlowVector();
                    motionX = flowVector.getX() * FLOW_MULTIPLIER;
                    motionZ = flowVector.getZ() * FLOW_MULTIPLIER;
                } else if (levelBlock.getId() == 9) {
                    this.motionX = this.getSpeed() * moveMultiplier * 0.05 * (x / diff);
                    this.motionZ = this.getSpeed() * moveMultiplier * 0.05 * (z / diff);
                    this.level.addParticle(new BubbleParticle(this.add(Utils.rand(-2.0, 2.0), Utils.rand(-0.5, 0), Utils.rand(-2.0, 2.0))));
                } else {
                    this.motionX = this.getSpeed() * moveMultiplier * 0.1 * (x / diff);
                    this.motionZ = this.getSpeed() * moveMultiplier * 0.1 * (z / diff);
                }
            }
            if (this.passengers.isEmpty() && (this.stayTime <= 0 || Utils.rand())) this.yaw = Math.toDegrees(-FastMath.atan2(x / diff, z / diff));
        }

        Vector3 before = this.target;
        this.checkTarget();
        if (this.target != null && before != this.target) {
            double x = this.target.x - this.x;
            double z = this.target.z - this.z;

            double diff = Math.abs(x) + Math.abs(z);
            if (!inWater && (this.stayTime > 0 || this.distance(this.target) <= ((this.getWidth()) / 2 + 0.05) * nearbyDistanceMultiplier())) {
                this.motionX = 0;
                this.motionZ = 0;
            } else {
                if (levelBlock.getId() == 8) {
                    BlockWater blockWater = (BlockWater) levelBlock;
                    Vector3 flowVector = blockWater.getFlowVector();
                    motionX = flowVector.getX() * FLOW_MULTIPLIER;
                    motionZ = flowVector.getZ() * FLOW_MULTIPLIER;
                } else if (levelBlock.getId() == 9) {
                    this.motionX = this.getSpeed() * moveMultiplier * 0.05 * (x / diff);
                    this.motionZ = this.getSpeed() * moveMultiplier * 0.05 * (z / diff);
                    this.level.addParticle(new BubbleParticle(this.add(Utils.rand(-2.0, 2.0), Utils.rand(-0.5, 0), Utils.rand(-2.0, 2.0))));
                } else {
                    this.motionX = this.getSpeed() * moveMultiplier * 0.15 * (x / diff);
                    this.motionZ = this.getSpeed() * moveMultiplier * 0.15 * (z / diff);
                }
            }
            if (this.passengers.isEmpty() && (this.stayTime <= 0 || Utils.rand())) this.yaw = Math.toDegrees(-FastMath.atan2(x / diff, z / diff));
        }

        if (this.followTarget != null || this.target != null) {
            Vector3 nowTarget = this.followTarget != null ? this.followTarget.clone() : this.target.clone();
            if (nowTarget instanceof Entity) {
                nowTarget.setY(nowTarget.getY() + ((Entity) nowTarget).getEyeHeight());
            }

            double dx = this.x - nowTarget.x;
            double dy = (this.y + this.getEyeHeight()) - nowTarget.y;
            double dz = this.z - nowTarget.z;
            double yaw = Math.asin(dx / Math.sqrt(dx * dx + dz * dz)) / Math.PI * 180.0D;
            double pitch = Math.round(Math.asin(dy / Math.sqrt(dx * dx + dz * dz + dy * dy)) / Math.PI * 180.0D);
            if (dz > 0.0D) {
                yaw = -yaw + 180.0D;
            }

            this.yaw = yaw;
            this.pitch = pitch;
        }else {
            this.pitch = 0;
        }

        double dx = this.motionX;
        double dz = this.motionZ;
        boolean isJump = this.checkJump(dx, dz);
        if (this.stayTime > 0 && !inWater) {
            this.stayTime -= tickDiff;
            this.move(0, this.motionY, 0);
        } else {
            Vector2 be = new Vector2(this.x + dx, this.z + dz);
            this.move(dx, this.motionY, dz);
            Vector2 af = new Vector2(this.x, this.z);

            if ((be.x != af.x || be.y != af.y) && !isJump) {
                this.moveTime -= 90;
            }
        }

        if (!isJump) {
            if (this.onGround && !inWater) {
                this.motionY = 0;
            } else if (this.motionY > -this.getGravity() * 4) {
                if (!(this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8), NukkitMath.floorDouble(this.z))) instanceof BlockLiquid)) {
                    this.motionY -= this.getGravity();
                }
            } else {
                this.motionY -= this.getGravity();
            }
        }
        this.updateMovement();
        if (this.route != null) {
            if (this.route.hasCurrentNode() && this.route.hasArrivedNode(this)) {
                if (this.route.hasNext()) {
                    this.target = this.route.next();
                }
            }
        }
        return this.followTarget != null ? this.followTarget : this.target;
    }

    public RouteFinder getRoute() {
        return this.route;
    }

    public void setRoute(RouteFinder route) {
        this.route = route;
    }

}
