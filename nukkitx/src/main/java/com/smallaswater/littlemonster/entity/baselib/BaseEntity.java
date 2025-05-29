package com.smallaswater.littlemonster.entity.baselib;


import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.entity.projectile.EntityArrow;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.entity.*;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HugeExplodeSeedParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.PlayerSkinPacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import cn.nukkit.network.protocol.SetEntityLinkPacket;
import cn.nukkit.potion.Effect;
import com.smallaswater.littlemonster.common.EntityTool;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.handle.DamageHandle;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import com.smallaswater.littlemonster.utils.Utils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author SmallasWater
 * Create on 2021/6/28 7:57
 * Package com.smallaswater.littlemonster.entity
 */
public abstract class BaseEntity extends EntityHuman {

    //如果主人死了 本体是否死亡
    @Setter
    public boolean deathFollowMaster = false;

    //准则 不会伤害主人
    @Setter
    @Getter
    protected EntityHuman masterHuman = null;

    protected float moveMultiplier = 1.0f;

    //停留
    int stayTime = 0;

    @Setter
    @Getter
    //目标
    Vector3 target = null;
    //锁定生物
    Entity followTarget = null;

    @Setter
    @Getter
    private boolean movement = true;

    protected final ConcurrentHashMap<EntityCreature, TargetWeighted> targetWeightedMap = new ConcurrentHashMap<>();

    protected ArrayList<BaseSkillManager> skillManagers = new ArrayList<>();

    protected ArrayList<BaseSkillManager> healthList = new ArrayList<>();

    protected int attackDelay = 0;

    protected int damageDelay = 0;

    protected int healDelay = 0;

    @Setter
    @Getter
    protected MonsterConfig config;

    boolean canAttack = true;

    @Setter
    @Getter
    protected int attackMode = ATTACK_MODE_MELEE;

    /**
     * 攻击模式 近战
     */
    public static final int ATTACK_MODE_MELEE = 0;
    /**
     * 攻击模式 范围
     */
    public static final int ATTACK_MODE_RANGE = 1;
    /**
     * 攻击模式 远程（弓箭）
     */
    public static final int ATTACK_MODE_ARROW = 2;
    /**
     * 攻击模式 触发 EntityInteractEvent 事件
     */
    public static final int ATTACK_MODE_EVENT = 3;

    /**
     * 攻击速度
     */
    @Getter
    public int entityAttackSpeed = 23;

    /**
     * 攻击力
     */
    @Getter
    public int damage = 2;

    /**
     * 移动速度
     */
    public float speed = 1.0f;

    public int seeSize = 20;

    public DamageHandle handle = new DamageHandle();

    /**
     * EntityMove 的等待时长
     * */
    public int waitTime = 0;

    BaseEntity(FullChunk chunk, CompoundTag nbt, @NotNull MonsterConfig config) {
        super(chunk, nbt);
        this.setConfig(config);
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

    public boolean hasNoTarget() {
        return getFollowTarget() == null || (getFollowTarget() != null && targetOption(getFollowTarget(), distance(getFollowTarget())));
    }

    public boolean isKnockback() {
        return this.attackTime > 0;
    }

    public double getSpeed() {
        return speed;
    }

    public Entity getFollowTarget() {
        return this.followTarget != null ? this.followTarget : (this.target instanceof EntityCreature ? (EntityCreature) this.target : null);
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
    protected boolean isPlayerTarget(Player player) {
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
        //不能攻击自己
        if (creature == this) {
            return true;
        }
        //不能攻击主人
        if (masterHuman != null && creature == masterHuman) {
            return true;
        }
        if (creature instanceof Player) {
            return !this.isPlayerTarget((Player) creature);
        } else {
            return creature.closed || !creature.isAlive() || creature.getLevel() != this.getLevel() || distance > seeSize;
        }
    }

    @Override
    public int getNetworkId() {
        return super.getNetworkId();
    }

    @Override
    protected float getBaseOffset() {
        return super.getBaseOffset();
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        this.level.addPlayerMovement(this, x, y, z, yaw, pitch, headYaw);
    }

    @Override
    public void spawnTo(Player player) {
        super.spawnTo(player);
        this.sendData(player);
        if (!this.hasSpawned.containsKey(player.getLoaderId()) && this.chunk != null && player.usedChunks.containsKey(Level.chunkHash(this.chunk.getX(), this.chunk.getZ()))) {
            this.hasSpawned.put(player.getLoaderId(), player);
            player.dataPacket(this.createAddEntityPacket());
            this.sendData(player);
        }
        if (this.riding != null) {
            this.riding.spawnTo(player);
            SetEntityLinkPacket pkk = new SetEntityLinkPacket();
            pkk.vehicleUniqueId = this.riding.getId();
            pkk.riderUniqueId = this.getId();
            pkk.type = 1;
            pkk.immediate = 1;
            player.dataPacket(pkk);
        }
    }

    @Override
    public void despawnFrom(Player player) {
        super.despawnFrom(player);

        if (this.hasSpawned.containsKey(player.getLoaderId())) {
            RemoveEntityPacket pk = new RemoveEntityPacket();
            pk.eid = this.getId();
            player.dataPacket(pk);
            this.hasSpawned.remove(player.getLoaderId());
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
        if (this instanceof LittleNpc && this.healDelay < 1000) {
            ++this.healDelay;
        }
        onUpdata();
        return true;
    }

    public abstract void onUpdata();

    public abstract void onAttack(EntityDamageEvent entity);

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (this.damageDelay >= config.getInvincibleTime()) {
            if (source.getAttackCooldown() >= this.config.getInvincibleTime()) {
                source.setAttackCooldown(this.config.getInvincibleTime());
            }
            this.damageDelay = 0;
            if (this.isKnockback() && source instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) source).getDamager() instanceof Player) {
                return false;
            } else if (this.fireProof && (source.getCause()
                    == EntityDamageEvent.DamageCause.FIRE || source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                    || source.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
                return false;
            } else {
                if (source instanceof EntityDamageByEntityEvent) {
                    ((EntityDamageByEntityEvent) source).setKnockBack(config.isKnock() ? 0.3F : 0);
                }
                this.target = null;
                this.stayTime = 0;
                if (super.attack(source)) {
                    this.onAttack(source);
                    return true;
                }
            }
        } else {
            source.setCancelled();
        }
        return false;
    }

    /**
     * 攻击生物
     *
     * @param entity 生物
     */
    public void attackEntity(EntityCreature entity) {
        if (this.attackDelay > getEntityAttackSpeed() && (entity.distance(this) <= getConfig().getAttackDistance() || entity.distance(this.add(0, this.getHealth(), 0)) <= getConfig().getAttackDistance())) {
            this.attackDelay = 0;
            this.waitTime = 0;
            switch (this.getAttackMode()) {
                case ATTACK_MODE_RANGE:
                    LinkedList<Entity> players = Utils.getAroundPlayers(this, config.getArea(), true, true, true);
                    for (Entity p : players) {
                        if (p instanceof Player && ((Player) p).isCreative()) {
                            continue;
                        }
                        if (p instanceof LittleNpc) {
                            continue;
                        }
                        p.attack(new EntityDamageByEntityEvent(this, p, EntityDamageEvent.DamageCause.ENTITY_ATTACK, getDamage(), (float) config.getKnockBack()));
                    }
                    entity.level.addParticle(new HugeExplodeSeedParticle(entity));
                    entity.level.addSound(entity, Sound.RANDOM_EXPLODE);
                    break;
                case ATTACK_MODE_ARROW:
                    double f = 1.3D;
                    Entity k = Entity.createEntity("Arrow", this.add(0, this.getEyeHeight(), 0), this);
                    if (!(k instanceof EntityArrow)) {
                        return;
                    }
                    EntityArrow arrow = (EntityArrow) k;
                    arrow.setMotion(
                            new Vector3(-Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * f * f,
                                    -Math.sin(Math.toRadians(pitch)) * f * f,
                                    Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * f * f));
                    EntityShootBowEvent ev = new EntityShootBowEvent(this, Item.get(ItemID.ARROW, 0, 1), arrow, f);
                    this.server.getPluginManager().callEvent(ev);
                    EntityProjectile projectile = ev.getProjectile();
                    if (ev.isCancelled()) {
                        projectile.kill();
                    } else {
                        ProjectileLaunchEvent launch = new ProjectileLaunchEvent(projectile);
                        this.server.getPluginManager().callEvent(launch);
                        if (launch.isCancelled()) {
                            projectile.kill();
                        } else {
                            EntityTool.setEntityCanBeSavedWithChunk(projectile, false);
                            projectile.spawnToAll();
                            ((EntityArrow) projectile).setPickupMode(EntityArrow.PICKUP_NONE);
                            this.level.addSound(this, Sound.RANDOM_BOW);
                        }
                    }
                    break;
                case ATTACK_MODE_EVENT: //触发EntityInteractEvent
//                  if (!hasBlockInLine(entity)) {
                    EntityInteractEvent event = new EntityInteractEvent(this, entity.getPosition().add(0.5, entity.getEyeHeight(), 0.5).getLevelBlock());
                    Server.getInstance().getPluginManager().callEvent(event);
//                  }
                    break;
                case ATTACK_MODE_MELEE:
                default:
                    HashMap<EntityDamageEvent.DamageModifier, Float> damage = new LinkedHashMap<>();
                    damage.put(EntityDamageEvent.DamageModifier.BASE, (float) getDamage());
                    if (entity instanceof Player) {
                        HashMap<Integer, Float> armorValues = new LinkedHashMap<Integer, Float>() {
                            {
                                this.put(298, 1.0F);
                                this.put(299, 3.0F);
                                this.put(300, 2.0F);
                                this.put(301, 1.0F);
                                this.put(302, 1.0F);
                                this.put(303, 5.0F);
                                this.put(304, 4.0F);
                                this.put(305, 1.0F);
                                this.put(314, 1.0F);
                                this.put(315, 5.0F);
                                this.put(316, 3.0F);
                                this.put(317, 1.0F);
                                this.put(306, 2.0F);
                                this.put(307, 6.0F);
                                this.put(308, 5.0F);
                                this.put(309, 2.0F);
                                this.put(310, 3.0F);
                                this.put(311, 8.0F);
                                this.put(312, 6.0F);
                                this.put(313, 3.0F);
                            }
                        };
                        float points = 0.0F;
                        for (Item i : ((Player) entity).getInventory().getArmorContents()) {
                            points += armorValues.getOrDefault(i.getId(), 0.0F);
                        }

                        damage.put(EntityDamageEvent.DamageModifier.ARMOR, (float) ((double) damage.getOrDefault(EntityDamageEvent.DamageModifier.ARMOR, 0.0F) - Math.floor((double) (damage.getOrDefault(EntityDamageEvent.DamageModifier.BASE, 1.0F) * points) * 0.04D)));
                    }

                    entity.attack(new EntityDamageByEntityEvent(this, entity, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage, (float) config.getKnockBack()));
                    break;
            }
            for (Effect effect : config.getEffects()) {
                entity.addEffect(effect);
            }
            //摆臂动作
            EntityEventPacket pk = new EntityEventPacket();
            pk.eid = this.getId();
            pk.event = EntityEventPacket.ARM_SWING;
            Server.broadcastPacket(this.getViewers().values(), pk);
        }
    }

    //判断中间是否有方块
    public boolean hasBlockInLine(Vector3 target) {
        for (Block block : this.getLineOfSight(Math.min((int) this.distance(target), 10))) {
            if (!block.isTransparent()) {
                return true;
            }
        }
        return false;
    }

    public float getMountedYOffset() {
        return getHeight() * 0.75F;
    }


    public int nearbyDistanceMultiplier() {
        return 1;
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

    @Override
    public void close() {
        super.close();
        if (this.targetWeightedMap != null) { //防止实体初始化时close导致报错
            this.targetWeightedMap.clear();
        }
        if (this.skillManagers!= null) {
            this.skillManagers.clear();
        }
        if (this.healthList!= null) {
            this.healthList.clear();
        }
    }

    /**
     * 获取死亡掉落经验值
     *
     * @return 经验值
     */
    public int deathDropExp() {
        if (config.getDropExp().size() > 1) {
            return Utils.rand(config.getDropExp().get(0), config.getDropExp().get(1));
        } else if (!config.getDropExp().isEmpty()) {
            return config.getDropExp().get(0);
        }
        return 0;
    }

    @NotNull
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

    @Override
    public void heal(float amount) {
        if (getHealth() < getMaxHealth()) {
            healthList.removeIf(i -> getHealth() + amount >= i.health);
            if (getHealth() + amount >= getMaxHealth()) {
                reset();
            }
        }
        this.heal(new EntityRegainHealthEvent(this, amount, 0));
    }

    public void reset() {
        handle = new DamageHandle();
        healthList = new ArrayList<>();
    }

    protected ArrayList<Player> getDamagePlayers() {
        ArrayList<Player> players = new ArrayList<>();
        Player player;
        for (String name : handle.playerDamageList.keySet()) {
            player = Server.getInstance().getPlayer(name);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    protected Player getDamageMax() {
        double max = 0;
        Player p = null;
        for (Map.Entry<String, Double> player : handle.playerDamageList.entrySet()) {
            if (player.getValue() > max) {
                if (Server.getInstance().getPlayer(player.getKey()) != null) {
                    p = Server.getInstance().getPlayer(player.getKey());
                }
                max = player.getValue();
            }
        }
        return p;
    }
}
