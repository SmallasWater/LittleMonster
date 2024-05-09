package com.smallaswater.littlemonster.entity.vanilla;

import cn.lanink.gamecore.utils.NukkitTypeUtils;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.mob.EntityMob;
import cn.nukkit.entity.passive.EntityAnimal;
import cn.nukkit.entity.passive.EntityBat;
import cn.nukkit.entity.passive.EntityParrot;
import cn.nukkit.entity.projectile.EntityArrow;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.entity.*;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HugeExplodeSeedParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.potion.Effect;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.EntityCommandSender;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.entity.baselib.BaseEntity;
import com.smallaswater.littlemonster.entity.vanilla.ai.entity.MovingVanillaEntity;
import com.smallaswater.littlemonster.handle.DamageHandle;
import com.smallaswater.littlemonster.manager.BossBarManager;
import com.smallaswater.littlemonster.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import nukkitcoders.mobplugin.entities.animal.WalkingAnimal;
import nukkitcoders.mobplugin.entities.monster.Monster;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.smallaswater.littlemonster.entity.baselib.BaseEntity.*;
import static com.smallaswater.littlemonster.entity.baselib.BaseEntity.ATTACK_MODE_MELEE;

public class VanillaOperateNPC extends MovingVanillaEntity {

    @Setter
    @Getter
    protected MonsterConfig config;

    //准则 不会伤害主人
    @Setter
    @Getter
    protected EntityHuman masterHuman = null;

    //攻击速度
    @Getter
    public int attackSleepTime = 23;

    protected int attackDelay = 0;

    protected int damageDelay = 0;

    protected int healDelay = 0;

    @Setter
    @Getter
    //伤害
    public int damage = 2;

    public int seeSize = 20;

    public DamageHandle handle = new DamageHandle();

    protected final ConcurrentHashMap<EntityCreature, TargetWeighted> targetWeightedMap = new ConcurrentHashMap<>();

    //目标
    Vector3 target_ = null;

    @Getter
    //锁定生物
    Entity followTarget = null;

    private Player boss = null;

    //停留
    int stayTime = 0;
    boolean canAttack = true;

    public VanillaOperateNPC(FullChunk chunk, CompoundTag nbt, MonsterConfig config) {
        super(chunk, nbt);
    }

    /**
     * 是否可以攻击目标实体 （主要为NPC配置文件规则限制）
     *
     * @param targetEntity 目标实体
     * @param isActive 是否为主动攻击
     * @return 是否可以攻击
     */
    protected boolean canAttackEntity(Entity targetEntity, boolean isActive) {
        if (this.targetOption(targetEntity, this.distance(targetEntity))) {
            return false;
        }
//        if (targetEntity instanceof LittleNpc) {
//            if (!Utils.canAttackNpc(this, (LittleNpc) targetEntity, false)) {
//                return true;
//            }
//        }
        if (!this.config.isActiveAttackEntity()) {
            if (!(targetEntity instanceof Player)) {
                return false;
            }
        }
        if (!this.config.isTargetPlayer() && isActive && targetEntity instanceof Player) {
            return false;
        }

        if (Server.getInstance().getPluginManager().getPlugin("MobPlugin") != null) {
            if (!config.isAttackFriendEntity()) {
                if (targetEntity instanceof WalkingAnimal) {
                    return false;
                }
            }
            if (!config.isAttackHostileEntity()) {
                if (targetEntity instanceof Monster) {
                    return false;
                }
            }
        }
        if (!config.isAttackFriendEntity()) {
            if (targetEntity instanceof EntityAnimal) {
                return false;
            }
        }
        if (!config.isAttackHostileEntity()) {
            return !(targetEntity instanceof EntityMob);
        }

        return true;
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

    @NotNull
    public TargetWeighted getTargetWeighted(EntityCreature entity) {
        if (!this.targetWeightedMap.containsKey(entity)) {
            this.targetWeightedMap.put(entity, new TargetWeighted());
        }
        return this.targetWeightedMap.get(entity);
    }

    /**
     * 寻找目标并锁定
     */
    protected void checkTarget(int currentTick) {
        if (isKnockback) {
            return;
        }

        if (currentTick % 15 == 0 || this.targetOption(this.followTarget)) {
            //扫描附近实体
            if (this.passengers.isEmpty()) {
                //获取范围内可以攻击的生物
                ArrayList<EntityCreature> scanEntities = new ArrayList<>();
                for (Entity entity : Utils.getAroundPlayers(this, seeSize, true, true, true)) {
                    //近战模式忽略部分会飞的实体 防止乱跑
                    //触发事件模式无法确定插件是近战还是远程 当作近战处理
                    //TODO 使用权重功能处理飞行生物，降低飞行生物目标权重
                    if (this.getConfig().getAttaceMode() == ATTACK_MODE_MELEE || this.getConfig().getAttaceMode() == ATTACK_MODE_EVENT) {
                        //忽略蝙蝠 鹦鹉
                        if (entity.getNetworkId() == EntityBat.NETWORK_ID || entity.getNetworkId() == EntityParrot.NETWORK_ID) {
                            continue;
                        }
                    }

                    if (entity instanceof EntityCreature && entity != this) {
                        if (this.canAttackEntity(entity, true)) {
                            scanEntities.add((EntityCreature) entity);
                            BaseEntity.TargetWeighted targetWeighted = this.getTargetWeighted((EntityCreature) entity);
                            targetWeighted.setReason(BaseEntity.TargetWeighted.REASON_AUTO_SCAN);
                        }
                    }
                }

                for (EntityCreature entity : this.targetWeightedMap.keySet()) {
                    if (!scanEntities.contains(entity) && this.targetOption(entity, this.distance(entity))) {
                        this.targetWeightedMap.remove(entity);
                    } else {
                        BaseEntity.TargetWeighted targetWeighted = this.getTargetWeighted(entity);
                        //更新距离
                        targetWeighted.setDistance(this.distance(entity));
                    }
                }

                ArrayList<EntityCreature> entities = new ArrayList<>(this.targetWeightedMap.keySet());
                entities.sort((p1, p2) -> Double.compare(this.getTargetWeighted(p2).getFinalWeighted() - this.getTargetWeighted(p1).getFinalWeighted(), 0.0D));
                if (!entities.isEmpty()) {
                    EntityCreature entity = entities.get(0);
                    if (entity != this.getFollowTarget()) {
                        if (canAttackEntity(entity, false)) {
                            this.setFollowTarget(entity, true);
                        }
                    }
                }
            }
        }
    }

    public void setFollowTarget(Entity target) {
        this.setFollowTarget(target, true);
    }

    public void setFollowTarget(Entity target, boolean attack) {
        this.followTarget = target;
        this.stayTime = 0;
        this.target_ = null;
        this.canAttack = attack;

    }

    /**
     * 攻击玩家
     */
    public void attackEntity(EntityCreature entity) {
        if (this.attackDelay > attackSleepTime && (entity.distance(this) <= this.getConfig().getAttackDistance() || entity.distance(this.add(0, this.getHealth(), 0)) <= this.getConfig().getAttackDistance())) {
            this.attackDelay = 0;
            switch (this.getConfig().getAttaceMode()) {
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
                    entity.getLevel().addParticle(new HugeExplodeSeedParticle(entity));
                    entity.getLevel().addSound(entity, Sound.RANDOM_EXPLODE);
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
                            if (NukkitTypeUtils.getNukkitType() == NukkitTypeUtils.NukkitType.MOT) {
                                projectile.setCanBeSavedWithChunk(false);
                            }
                            projectile.spawnToAll();
                            ((EntityArrow) projectile).setPickupMode(EntityArrow.PICKUP_NONE);
                            this.getLevel().addSound(this, Sound.RANDOM_BOW);
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

    protected void disCommand(String cmd) {
        disCommand(cmd, null, null);
    }

    protected void disCommand(String cmd, String target, String name) {
        if (target != null) {
            Server.getInstance().getCommandMap().dispatch(
                    new EntityCommandSender(getName()), cmd.replace(target, name));
        } else {
            Server.getInstance().getCommandMap().dispatch(new EntityCommandSender(getName()), cmd);
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
    @Override
    public int getNetworkId() {
        return 0;
    }

    @Override
    public void close() {
        if (boss != null) {
            BossBarManager.BossBarApi.removeBossBar(boss);
            boss = null;
        }
        if (config != null) {
            if (config.isDisplayDamage() && this.handle != null) {
                handle.display();
            }
        }

        //如果在类实例化时调用onClose方法 这些变量可能为null
        //noinspection ConstantConditions
        if (this.targetWeightedMap != null) {
            this.targetWeightedMap.clear();
        }
//        if (this.skillManagers != null) {
//            this.skillManagers.clear();
//        }
//        if (this.healthList != null) {
//            this.healthList.clear();
//        }
        this.handle = null;
        this.route = null;
        super.close();
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        super.entityBaseTick(tickDiff);
        if (!this.isAlive()) {
            this.close();
        }

        if (this.attackDelay < 1000) {
            ++this.attackDelay;
        }
        if (this.damageDelay < 1000) {
            ++this.damageDelay;
        }
        if (this.healDelay < 1000) {
            ++this.healDelay;
        }
        return true;
    }
}
