package com.smallaswater.littlemonster.entity.vanilla;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.BlockRedstone;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.entity.mob.EntityMob;
import cn.nukkit.event.entity.*;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.MobArmorEquipmentPacket;
import cn.nukkit.network.protocol.MobEquipmentPacket;
import cn.nukkit.utils.TextFormat;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.IEntity;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.entity.baselib.BaseEntity;
import com.smallaswater.littlemonster.entity.vanilla.ai.ShootAttackExecutor;
import com.smallaswater.littlemonster.entity.vanilla.ai.route.AdvancedRouteFinder;
import com.smallaswater.littlemonster.events.entity.LittleMonsterEntityDeathDropExpEvent;
import com.smallaswater.littlemonster.items.BaseItem;
import com.smallaswater.littlemonster.items.DeathCommand;
import com.smallaswater.littlemonster.items.DropItem;
import com.smallaswater.littlemonster.skill.BaseSkillAreaManager;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import com.smallaswater.littlemonster.skill.defaultskill.AttributeHealthSkill;
import com.smallaswater.littlemonster.skill.defaultskill.MessageHealthSkill;
import com.smallaswater.littlemonster.skill.defaultskill.SummonHealthSkill;
import com.smallaswater.littlemonster.manager.BossBarManager;
import com.smallaswater.littlemonster.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.smallaswater.littlemonster.entity.baselib.BaseEntity.ATTACK_MODE_ARROW;

public class VanillaNPC extends BaseVanillaNPC implements IEntity {
    public static final String TAG = "LittleMonster";

    @Setter
    @Getter
    private int liveTime = -1;

    //如果主人死了 本体是否死亡。用于由技能生成的怪物
    @Setter
    public boolean deathFollowMaster = false;

    public String spawnPos = null;

    public int strollTick = 0;

    // 防具
    @Setter
    private Item[] armor;

    // 手持装备
    @Setter
    private Item tool;

    // 下方是 BlocklyNukkit 实现

    public boolean enableAttack = true;// 将会受到攻击
    public boolean enableHurt = true;// 受伤的红温
    public double g = 10d;// 重力
    public boolean enableKnockBack = true;
    public double knockBase = 0.4d;

    //public boolean isjumping = false;
    public double jumpHigh = 1.26;
    //public boolean isonRoute = false;
    //public Vector3 nowtarget = null;
    public double speed = 3;
    //public int actions = 0;
    //public Vector3 actioinVec = new Vector3();
    public int routeMax = 80;
    //public Vector3 previousTo = null;

    public float halfWidth = 0.3f;
    public float width = 0.6f;
    public float length = 0.6f;
    public float height = 1.8f;
    public float eyeHeight = 1.62f;

    public final int networkId;

    public VanillaNPC(FullChunk chunk, CompoundTag nbt, MonsterConfig config, Boolean skip) {
        super(chunk, nbt, config);
        this.networkId = config.getNetworkId();
        if (skip) return;
        Entity temp = Entity.createEntity(String.valueOf(config.getNetworkId()), chunk, nbt);
        if (temp != null) {
            width = temp.getWidth();
            length = temp.getLength();
            height = temp.getHeight();
            // length 可能为0，详见 `AxisAlignedBB bb` 计算
            if (length == 0) {
                length = width;
            }
            eyeHeight = temp.getEyeHeight();
            halfWidth = this.getWidth() / 2;
            temp.close();
        }
        this.dataProperties.putFloat(DATA_BOUNDING_BOX_HEIGHT, getHeight());
        this.dataProperties.putFloat(DATA_BOUNDING_BOX_WIDTH, getWidth());

        this.setMaxHealth(config.getHealth());
        this.setHealth(config.getHealth());
        this.namedTag.putString(TAG, config.getName());
        this.setNameTagVisible(true);
        this.setNameTagAlwaysVisible(true);
        this.loadSkill();
        vanillaNPC = this;
        if (config.getAttackMode() == ATTACK_MODE_ARROW) {
            shootAttackExecutor = new ShootAttackExecutor();
        }
    }

    @Override
    public void initEntity() {
        super.initEntity();
    }

    @Override
    public String getName() {
        return "VanillaNPC";
    }

    @Override
    public int getNetworkId() {
        return networkId;
    }

    @Override
    public float getGravity() {
        return (float) g / 20;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getLength() {
        return length;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public float getEyeHeight() {
        return eyeHeight;
        //return 0.0F;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setLength(float length) {
        this.length = length;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setEyeHeight(float eyeHeight) {
        this.eyeHeight = eyeHeight;
    }

    @Override
    public float getMovementSpeed() {
        return (float) this.speed / 30;
    }

    @Override
    public void knockBack(Entity attacker, double damage, double x, double z, double base) {
        isKnockback = true;
        double f = Math.sqrt(x * x + z * z);
        if (f <= 0) {
            return;
        }

        f = 1 / f;

        Vector3 motion = new Vector3(this.motionX, this.motionY, this.motionZ);

        motion.x /= 2d;
        motion.y /= 2d;
        motion.z /= 2d;
        motion.x += x * f * base;
        motion.y += base;
        motion.z += z * f * base;

        if (motion.y > base) {
            motion.y = base;
        }

        this.move(motion.x, motion.y + 0.55, motion.z);
        this.updateMovement();
        //super.knockBack(attacker, damage, x, z, base);
    }

    @Override
    public void spawnTo(Player player) {
        super.spawnTo(player);

        // 发送盔甲
        if (!this.armor[0].isNull() || !this.armor[1].isNull() || !this.armor[2].isNull() || !this.armor[3].isNull()) {
            MobArmorEquipmentPacket pk = new MobArmorEquipmentPacket();
            pk.eid = this.getId();
            pk.slots = this.armor;

            player.dataPacket(pk);
        }

        // 发送武器
        if (this.tool != null) {
            MobEquipmentPacket pk = new MobEquipmentPacket();
            pk.eid = this.getId();
            pk.hotbarSlot = 0;
            pk.item = this.tool;
            player.dataPacket(pk);
        }
    }

    private Player boss = null;

    @Override
    public boolean onUpdate(int currentTick) {
        // 技能召唤的
        if (this.deathFollowMaster && masterHuman != null) {
            if (masterHuman.isClosed()) {
                this.close();
                return false;
            }
        }
        // 丢失配置时
        if (config == null) {
            this.close();
            return false;
        }
        // 更新名字
        this.setNameTag(config.getTag()
                .replace("{名称}", getConfig().getName())
                .replace("{血量}", getHealth() + "")
                .replace("{最大血量}", getMaxHealth() + ""));
        onHealthListener((int) Math.floor(getHealth()));

//        // 更新乘客
//        try {
//            for (Entity entity : this.getPassengers()) {
//                if (entity.distance(this) > 3) {
//                    this.setEntityRideOff(entity);
//                }
//            }
//        } catch (java.util.ConcurrentModificationException ignored) {}

//        //处理骑乘
//        this.updatePassengers();
//
//        if (currentTick % 10 == 0) {
//            if (this.getFollowTarget() != null) {
//                lookAt(this.getFollowTarget());
//            }
//            this.getLevel().addParticle(new HappyVillagerParticle(this.getPosition().add(0, getHeight())));
//        }
        checkTarget(currentTick);

        if (this.getFollowTarget() == null) {
            // 随机移动
            strollMoveHandle(currentTick);
        } else {
            if (shootAttackExecutor != null &&
                    shootAttackExecutor.inExecute &&
                    shootAttackExecutor.failedAttackCount < 2) {// 在射箭过程中且失败次数小于2 则停止移动。
                stopMove();
            } else if (this.route.getDestination() == null ||
                    this.route.getDestination().distance(this.getFollowTarget().getPosition()) > this.getConfig().getAttackDistance()) {
                // 防抖（怪物走路时频繁回头的问题）
                findAndMove(this.getFollowTarget().getPosition());
            }

            // 攻击目标实体
            if (this.getFollowTarget() instanceof EntityCreature) {
                if (this.targetOption(this.getFollowTarget(), this.distance(this.getFollowTarget()))) {
                    this.setFollowTarget(null, false);
                    return true;
                }
                if (this.getFollowTarget() instanceof Player) {
                    Player player = (Player) this.getFollowTarget();
                    if (this.getFollowTarget() != this.followTarget || this.canAttack) {
                        int atkResult = this.attackEntity(player);
                        if (atkResult == 2 && shootAttackExecutor != null) {// 为 2 代表因为攻击范围不够导致失败
                            findAndMove(this.getFollowTarget());
                        }
                    }
                } else {
                    if (this.canAttack) {
                        int atkResult = this.attackEntity((EntityCreature) this.getFollowTarget());
                        if (atkResult == 2 && shootAttackExecutor != null) {
                            findAndMove(this.getFollowTarget());
                        }
                    }
                }
            } else if (this.getFollowTarget() != null && this.distance(this.getFollowTarget()) > this.seeSize) {
                this.setFollowTarget(null);
            }
        }
        // 调用nk预设函数
        return super.onUpdate(currentTick);
    }

    @Override
    public void reset() {
        super.reset();
        if (config != null) {
            config.vanillaSetting(this);
        }
    }

    public void strollMoveHandle(int currentTick) {
        if (!getConfig().isCanMove()) return;
        if (currentTick % 20 != 0) {
            return;
        }
        // 处理随机移动
        if (strollTick > 0) {
            strollTick--;
            return;
        }
        strollTick = Utils.rand(10, 25);

        CompletableFuture.runAsync(() -> {// 似乎会过度消耗性能，如果并行执行无法优化，请考虑回退。
            Position nodeNext = Pathfinder.getNode(this.getPosition(), 6);
            if (nodeNext == null) {
                // 随机移动失败，传送回重生点
                this.teleport(LittleMonsterMainClass.getInstance().positions.get(getSpawnPos()).getSpawnPos());
            } else {
                this.stopMove();
                this.lookAt(nodeNext);
                directMove(nodeNext);
            }
        });
    }

    public void onAttack(EntityDamageEvent sure) {
        if (isImmobile() && !config.isImmobile() && !LittleMonsterMainClass.hasRcRPG) {
            sure.setCancelled();
        }
        if (sure instanceof EntityDamageByEntityEvent) {
            if (config.isPassiveAttackEntity()) {
                if (((EntityDamageByEntityEvent) sure).getDamager() instanceof Player) {
                    Player player = (Player) ((EntityDamageByEntityEvent) sure).getDamager();
                    if (!targetOption(player, this.distance(player))) {
                        this.getTargetWeighted(player).setReason(BaseEntity.TargetWeighted.REASON_PASSIVE_ATTACK_ENTITY);
                    }

                } else {
                    Entity damager = ((EntityDamageByEntityEvent) sure).getDamager();
                    if (!config.isAttackHostileEntity()) {
                        if (damager instanceof EntityMob) {
                            return;
                        }
                    }
                    if (damager instanceof LittleNpc) {
                        if (!Utils.canAttackNpc(this, (LittleNpc) damager, true)) {
                            return;
                        }
                    }
                    if (!targetOption(damager, distance(damager)) && damager instanceof EntityCreature) {
                        this.getTargetWeighted((EntityCreature) damager).setReason(BaseEntity.TargetWeighted.REASON_PASSIVE_ATTACK_ENTITY);
                    }
                }
            }
            if (LittleMonsterMainClass.hasRcRPG) {
                return;// 有 RcRPG 时无需处理攻击事件
            }
            if (((EntityDamageByEntityEvent) sure).getDamager() instanceof Player) {
                Player player = (Player) ((EntityDamageByEntityEvent) sure).getDamager();
                this.handle.add(player.getName(), sure.getFinalDamage());
            }
            return;
        }
        this.level.addParticle(new DestroyBlockParticle(this, new BlockRedstone()));
    }

    /**
     * 受到攻击
     */
    @Override
    public boolean attack(EntityDamageEvent source) {
        this.updateMovement();
        if (this.damageDelay < config.getInvincibleTime()) {// 无敌时间
            source.setCancelled();
            return false;
        }
        if (source.getAttackCooldown() >= this.config.getInvincibleTime()) {
            source.setAttackCooldown(this.config.getInvincibleTime());
        }

        // 窒息伤害
        if (source.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            source.setCancelled();
            this.strollTick = 0;
            return false;
        }
        //LittleMonsterMainClass.getInstance().getLogger().info("受到攻击 "+source.getCause());

        this.damageDelay = 0;
        if (enableHurt) {
            this.displayHurt();
        }
        if (!enableAttack) {
            return false;
        }

        if (enableKnockBack) {
            if (source instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) source).getDamager();
                this.knockBack(damager, source.getFinalDamage(), -(damager.x - this.x), -(damager.z - this.z), knockBase);
            }
        }
        if (super.attack(source)) {
            this.onAttack(source);
            return true;
        } else {
            return false;
        }
        // 上面的实现完全等价与下方代码时，需删除冗余
//        if (this.damageDelay >= config.getInvincibleTime()) {
//            if (source.getAttackCooldown() >= this.config.getInvincibleTime()) {
//                source.setAttackCooldown(this.config.getInvincibleTime());
//            }
//            this.damageDelay = 0;
//            if (isKnockback && source instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent) source).getDamager() instanceof Player) {
//                return false;
//            } else if (this.fireProof && (source.getCause()
//                    == EntityDamageEvent.DamageCause.FIRE || source.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
//                    || source.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
//                return false;
//            } else {
//                if (source instanceof EntityDamageByEntityEvent) {
//                    ((EntityDamageByEntityEvent) source).setKnockBack(config.isKnock() ? 0.3F : 0);
//                }
//                this.stayTime = 0;
//                if (super.attack(source)) {
//                    this.onAttack(source);
//                    return true;
//                }
//            }
//        } else {
//            source.setCancelled();
//        }
//        return false;
    }

    @Override
    public void close() {
        super.close();
    }

    public void turnRound(double yaw) {
        this.yaw += yaw;
    }

    public void headUp(double pitch) {
        this.pitch += pitch;
    }

    public void setEnableAttack(boolean attack) {
        this.enableAttack = attack;
    }

    public void setEnableAttack() {
        this.setEnableAttack(true);
    }

    public void setEnableHurt(boolean hurt) {
        this.enableHurt = hurt;
    }

    public void setEnableHurt() {
        this.setEnableHurt(true);
    }

    public void setG(double newg) {
        this.g = newg;
    }

    public void setSneak(boolean sneak) {
        this.setSneaking(sneak);
    }

    public void setSneak() {
        this.setSneaking(!this.isSneaking());
    }

    public void setJumpHigh(double j) {
        this.jumpHigh = j;
    }

    public void setEnableKnockBack(boolean knock) {
        this.enableKnockBack = knock;
    }

    public void setEnableKnockBack() {
        this.setEnableKnockBack(true);
    }

    public void setKnockBase(double base) {
        this.knockBase = base;
    }

    public void setSpeed(double s) {
        this.speed = s;
    }

    public void setRouteMax(int m) {
        this.routeMax = m;
        this.route.setSearchLimit(m);
    }

    public void setSwim(boolean swim) {
        this.setSwimming(swim);
    }

    public void setSwim() {
        this.setSwim(!this.isSwimming());
    }

    public void displayHurt() {
        EntityEventPacket pk = new EntityEventPacket();
        pk.eid = this.id;
        pk.event = EntityEventPacket.HURT_ANIMATION;
        this.getLevel().getPlayers().values().forEach((player -> player.dataPacket(pk)));
    }

    public void displaySwing() {
        EntityEventPacket pk = new EntityEventPacket();
        pk.eid = this.id;
        pk.event = EntityEventPacket.ARM_SWING;
        this.getLevel().getPlayers().values().forEach((player -> player.dataPacket(pk)));
    }

    @Override
    public void jump() {
        if (this.onGround) {
            this.motionY = jumpHigh;
        }
    }

    public void lookAt(Position pos) {
        double xdiff = pos.x - this.x;
        double zdiff = pos.z - this.z;
        double angle = Math.atan2(zdiff, xdiff);
        double yaw = ((angle * 180) / Math.PI) - 90;
        double ydiff = pos.y - this.y;
        Vector2 v = new Vector2(this.x, this.z);
        double dist = v.distance(pos.x, pos.z);
        angle = Math.atan2(dist, ydiff);
        double pitch = ((angle * 180) / Math.PI) - 90;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public List<Player> getPlayersIn(double distance) {
        ArrayList<Player> players = new ArrayList<>();
        for (Player p : Server.getInstance().getOnlinePlayers().values()) {
            if (!p.getLevel().getName().equals(this.getLevel().getName())) {
                continue;
            } else {
                double d = this.distance(p);
                if (d < distance) {
                    players.add(p);
                }
            }
        }
        return players;
    }

    public List<Entity> getEntitiesIn(double distance) {
        ArrayList<Entity> entities = new ArrayList<>();
        for (Entity e : this.getLevel().getEntities()) {
            if (e.distance(this) <= distance) {
                entities.add(e);
            }
        }
        return entities;
    }

    public boolean isSneak() {
        return this.isSneaking();
    }

    public boolean canMoveTo(Position to) {
        AdvancedRouteFinder finder = new AdvancedRouteFinder(this);
        finder.setStart(this);
        finder.setDestination(to);
        finder.setSearchLimit(routeMax);
        finder.setLevel(to.getLevel());
        finder.search();
        return finder.isSuccess();
    }

    public boolean findAndMove(Position to) {
        this.route = new AdvancedRouteFinder(this);
        this.route.setStart(this);
        this.route.setDestination(to);
        this.route.setSearchLimit(routeMax);
        this.route.setLevel(to.getLevel());
        this.route.search();
        this.setTarget(to, true);
        return this.route.isSuccess();
    }

    public boolean directMove(Position to) {
        this.setTarget(to, true);
        return true;
    }

    public void stopMove() {
        this.route.forceStop();

    }

    public void hit(Entity entity) {
        this.displaySwing();
        entity.attack(new EntityDamageByEntityEvent(this, entity, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1f, 0.5f));
    }

    public void setEntityRideOn(Entity entity) {
        this.mountEntity(entity);
    }

    public void isEntityRideOn(Entity entity) {
        this.isPassenger(entity);
    }

    public void setEntityRideOff(Entity entity) {
        entity.riding = null;
        this.dismountEntity(entity);
        entity.setPosition(this);
        this.getPassengers().clear();
        this.updatePassengers();
    }

    public Player getRidingPlayer() {
        for (Entity entity : this.getPassengers()) {
            if (entity instanceof Player) {
                return (Player) entity;
            }
        }
        return null;
    }

    @Override
    public boolean isVanillaEntity() {
        return true;
    }

    @Override
    public String getSpawnPos() {
        return spawnPos;
    }

    @Override
    public void setSpawnPos(String name) {
        spawnPos = name;
    }

    @Override
    public void setAttackDamage(int damage) {
        this.damage = damage;
    }

    @Override
    public void setEntityAttackSpeed(int speed) {
        this.entityAttackSpeed = speed;
    }

    @Override
    public void onDeath(EntityDeathEvent event) {
        Entity damager = null;
        EntityDamageEvent d = event.getEntity().getLastDamageCause();
        if (d instanceof EntityDamageByEntityEvent) {
            damager = ((EntityDamageByEntityEvent) d).getDamager();
        }
        LinkedList<Item> items = new LinkedList<>();
        // 死亡执行命令
        for (DeathCommand command : getConfig().getDeathCommand()) {
            if (command.getRound() >= Utils.rand(1, 100)) {
                String cmd = command.getCmd();
                cmd = cmd.replace("{x}", String.format("%.2f", getX()))
                        .replace("{y}", String.format("%.2f", getY()))
                        .replace("{z}", String.format("%.2f", getZ()))
                        .replace("{level}", getLevel().getFolderName());
                if (cmd.contains("@" + BaseItem.TARGETALL)) {
                    for (Player player : getDamagePlayers()) {
                        disCommand(cmd, "@" + BaseItem.TARGETALL, player.getName());
                    }

                } else {
                    if (cmd.contains("@" + BaseItem.TARGET)) {
                        if (damager instanceof Player) {
                            cmd = cmd.replace("@" + BaseItem.TARGET, damager.getName());
                        }
                    }
                    if (cmd.contains("@" + BaseItem.DAMAGE)) {
                        Player player = getDamageMax();
                        if (player != null) {
                            cmd = cmd.replace("@" + BaseItem.DAMAGE, player.getName());
                        }
                    }
                    disCommand(cmd);
                }
            }
        }

        // 死亡掉落物品
        for (DropItem key : getConfig().getDeathItem()) {
            if (key.getRound() >= Utils.rand(1, 100)) {
                items.add(key.getItem());
            }
        }
        event.setDrops(items.toArray(new Item[0]));

        String deathMessage = getConfig().getConfig().getString("公告.死亡.信息", "&e[ &bBOSS &e] {name} 在坐标: x: {x} y: {y} z: {z} 处死亡");
        if (getConfig().getConfig().getBoolean("公告.死亡.是否提示", true)) {
            Server.getInstance().broadcastMessage(TextFormat.colorize('&', deathMessage.replace("{name}", getConfig().getName())
                    .replace("{x}", getFloorX() + "")
                    .replace("{y}", getFloorY() + "")
                    .replace("{z}", getFloorZ() + "")
                    .replace("{level}", getLevel().getFolderName())));
        }
        if (damager != null) {
            if (damager instanceof Player) {
                String killMessage = getConfig().getConfig().getString("公告.击杀.信息", "&e[ &bBOSS提醒 &e] &d{name} 被 {player} 击杀");
                if (getConfig().getConfig().getBoolean("公告.击杀.是否提示", true)) {
                    Server.getInstance().broadcastMessage(TextFormat.colorize('&', killMessage
                            .replace("{name}", getConfig().getName())
                            .replace("{player}", damager.getName()))
                    );
                }

                LittleMonsterEntityDeathDropExpEvent expEvent = new LittleMonsterEntityDeathDropExpEvent(this, this.deathDropExp(), d);
                Server.getInstance().getPluginManager().callEvent(expEvent);
                if (!expEvent.isCancelled()) {
                    String tipText;
                    if (expEvent.getDifference() > 0) {
                        tipText = "经验 +" + expEvent.getOriginExp() + "§a(" + expEvent.getDifference() + ")";
                    } else {
                        tipText = "经验 +" + expEvent.getTotalExp();
                    }
                    if (expEvent.getTotalExp() > 0) {
                        ((Player) damager).addExperience(expEvent.getTotalExp());// TODO:升级音效
                        ((Player) damager).sendActionBar(tipText);
                    }
                }
            }
        }
    }

    @Override
    public Entity getEntity() {
        return this;
    }

    @Override
    public void setSkinByIEntity(Skin skin) {
        // ignore
    }

    @Override
    public ArrayList<BaseSkillManager> getSkillManagers() {
        return skillManagers;
    }

    public ArrayList<BaseSkillManager> getHealthSkill(int health) {
        ArrayList<BaseSkillManager> skillManagers = new ArrayList<>();
        for (BaseSkillManager skillManager : getSkillManagers()) {
            if (skillManager.health < health) continue;
            if (!healthList.contains(skillManager.health)) {
                skillManagers.add(skillManager);
                healthList.add(skillManager.health);
            }
        }
        return skillManagers;
    }

    public int healSettingTime;

    public int heal;

    public void onHealthListener(int health) {
        // 血量技能处理
        ArrayList<BaseSkillManager> skillAreaManagers = getHealthSkill(health);
        if (!skillAreaManagers.isEmpty()) {
            for (BaseSkillManager skillManager : skillAreaManagers) {
                if (skillManager instanceof BaseSkillAreaManager) {
                    if (getFollowTarget() == null) continue;
                    if (targetOption(this.getFollowTarget(), distance(getFollowTarget()))) continue;
                    if (skillManager.mode == 1) {
                        skillManager.display(Utils.getAroundPlayers(this, config.getArea(), true, true, true).toArray(new Entity[0]));
                    } else {
                        if (getFollowTarget() instanceof Player) {
                            skillManager.display(getFollowTarget());
                        }
                    }
                } else {
                    if (skillManager instanceof AttributeHealthSkill) {
                        skillManager.display((Player) null);
                    }
                    if (skillManager instanceof SummonHealthSkill) {
                        skillManager.display(this.getEntity());
                    }
                    if (skillManager instanceof MessageHealthSkill) {
                        skillManager.display(getDamagePlayers().toArray(new Player[0]));
                    }
                }
            }
        }
        // BossBar 处理
        if (this.getFollowTarget() != null && this.getFollowTarget() instanceof Player) {
            if (healDelay >= healSettingTime &&
                    heal > 0 &&
                    !config.isUnFightHeal()) {
                healDelay = 0;
                this.heal(heal);
            }
            if (config.isShowBossBar()) {
                if (targetOption(this.getFollowTarget(), this.distance(this.getFollowTarget()))) {
                    if (boss != null) {
                        BossBarManager.BossBarApi.removeBossBar(boss);
                        boss = null;
                    }
                    return;
                }
                if (this.getFollowTarget() instanceof Player) {
                    boss = (Player) this.getFollowTarget();
                    if (!BossBarManager.BossBarApi.hasCreate((Player) this.getFollowTarget(), getId())) {
                        BossBarManager.BossBarApi.createBossBar((Player) this.getFollowTarget(), getId());
                    }
                    BossBarManager.BossBarApi.showBoss((Player) getFollowTarget(),
                            getNameTag(),
                            getHealth(),
                            getMaxHealth());
                }
            }
        } else {
            if (getFollowTarget() == null || !config.isUnFightHeal()) {
                if (healDelay >= healSettingTime && heal > 0) {
                    healDelay = 0;
                    this.heal(heal);
                }
            }
            if (config.isShowBossBar()) {
                if (boss != null) {
                    BossBarManager.BossBarApi.removeBossBar(boss);
                    boss = null;
                }
            }
        }
    }
}
