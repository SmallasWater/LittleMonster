package com.smallaswater.littlemonster.entity.vanilla;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.entity.*;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HappyVillagerParticle;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.utils.TextFormat;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.EntityCommandSender;
import com.smallaswater.littlemonster.entity.IEntity;
import com.smallaswater.littlemonster.entity.vanilla.ai.route.AdvancedRouteFinder;
import com.smallaswater.littlemonster.events.entity.LittleMonsterEntityDeathDropExpEvent;
import com.smallaswater.littlemonster.items.BaseItem;
import com.smallaswater.littlemonster.items.DeathCommand;
import com.smallaswater.littlemonster.items.DropItem;
import com.smallaswater.littlemonster.manager.BossBarManager;
import com.smallaswater.littlemonster.threads.runnables.RouteFinderRunnable;
import com.smallaswater.littlemonster.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.smallaswater.littlemonster.entity.baselib.BaseEntity.*;

public class VanillaNPC extends VanillaOperateNPC implements IEntity {
    @Setter
    @Getter
    private int liveTime = -1;

    //如果主人死了 本体是否死亡。用于由技能生成的怪物
    @Setter
    public boolean deathFollowMaster = false;

    public String spawnPos = null;

    protected int attackDelay = 0;


    // 下方是 BlocklyNukkit 实现

    public VanillaNPC vanillaNPC;

    public Vector3 dvec = new Vector3(0, 0, 0);

    public List<Item> extraDropItems = new ArrayList<>();
    public boolean dropHand = false;
    public boolean dropOffhand = false;
    public List<Integer> dropSlot = new ArrayList<>();

    public boolean enableAttack = true;// 将会受到攻击
    public boolean enableHurt = true;// 受伤的红温
    public double g = 10d;// 重力
    public boolean enableKnockBack = true;
    public double knockBase = 0.4d;

    public boolean isjumping = false;
    public double jumphigh = 1;
    public boolean isonRoute = false;
    public Vector3 nowtarget = null;
    public double speed = 3;
    public int actions = 0;
    public Vector3 actioinVec = new Vector3();
    public int routeMax = 50;
    public Vector3 previousTo = null;
    public boolean justDamaged = false;

    public float halfWidth = 0.3f;
    public float width = 0.6f;
    public float length = 0.6f;
    public float height = 1.8f;
    public float eyeHeight = 1.62f;

    public final int networkId;

    public VanillaNPC(FullChunk chunk, CompoundTag nbt, MonsterConfig config) {
        super(chunk, nbt, config);
        this.networkId = config.getNetworkId();
        Entity temp = Entity.createEntity(String.valueOf(config.getNetworkId()), chunk, nbt);
        if (temp != null) {
            width = temp.getWidth();
            length = temp.getLength();
            height = temp.getHeight();
            eyeHeight = temp.getEyeHeight();
            halfWidth = this.getWidth() / 2;
            temp.close();
        }
        this.dataProperties.putFloat(DATA_BOUNDING_BOX_HEIGHT, getHeight());
        this.dataProperties.putFloat(DATA_BOUNDING_BOX_WIDTH, getWidth());

        this.setNameTagVisible(true);
        this.setNameTagAlwaysVisible(true);
        this.setScale(1.0f);
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
    public boolean onUpdate(int currentTick) {
        //更新乘客
        try {
            for (Entity entity : this.getPassengers()) {
                if (entity.distance(this) > 3) {
                    this.setEntityRideOff(entity);
                }
            }
        } catch (java.util.ConcurrentModificationException e) {
            //ignore
        }
        //处理骑乘
        this.updatePassengers();

        if (currentTick % 10 == 0) {
            this.getLevel().addParticle(new HappyVillagerParticle(this.getPosition().add(0, getHeight())));
        }
        checkTarget(currentTick);

        if (this.getFollowTarget() != null) {
            findAndMove(this.getFollowTarget().getPosition());// 移动处理
            if (this.getFollowTarget().distance(this.getPosition()) < 3) {
                canAttackEntity(this.getFollowTarget(), true);
            }
        }

        //攻击目标实体
        if(this.getFollowTarget() instanceof EntityCreature) {
            if (this.targetOption(this.getFollowTarget(), this.distance(this.getFollowTarget()))) {
                this.setFollowTarget(null,false);
                return true;
            }
            if(this.getFollowTarget() instanceof Player) {
                Player player = (Player) this.getFollowTarget();
                if (this.getFollowTarget() != this.followTarget || this.canAttack) {
                    this.attackEntity(player);
                }
            } else {
                if (this.canAttack) {
                    this.attackEntity((EntityCreature) this.getFollowTarget());
                }
            }
        } else if (this.getFollowTarget() != null && this.distance(this.getFollowTarget()) > this.seeSize) {
            this.setFollowTarget(null);
        }
        //调用nk预设函数
        return super.onUpdate(currentTick);
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        this.updateMovement();
        if (enableHurt) {
            this.displayHurt();
        }
        if (enableAttack) {
            if (enableKnockBack) {
                justDamaged = true;
                if (source instanceof EntityDamageByEntityEvent) {
                    Entity damager = ((EntityDamageByEntityEvent) source).getDamager();
                    this.knockBack(damager, source.getFinalDamage(), -(damager.x - this.x), -(damager.z - this.z), knockBase);
                }
            }
            return super.attack(source);
        } else {
            return true;
        }
    }

    @Override
    public void close() {
        List<Item> tmp = new ArrayList<>(extraDropItems);
        tmp.forEach(each -> vanillaNPC.getLevel().dropItem(vanillaNPC, each));
        super.close();
    }

    public void addExtraDropItem(Item item) {
        this.extraDropItems.add(item);
    }

    public boolean hasDropItem(Item item) {
        if (dropHand) {
            return true;
        } else if (dropOffhand) {
            return true;
        } else {
            for (Item i : this.extraDropItems) {
                if (item.equals(i, true, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void removeExtraDropItem(Item item) {
        this.extraDropItems.remove(item);
    }

    public Item[] getExtraDropItems() {
        return this.extraDropItems.toArray(new Item[0]);
    }

    public Item[] getDropItems() {
        List<Item> tmp = new ArrayList<>(extraDropItems);
        return tmp.toArray(new Item[0]);
    }

    public void setDropHand(boolean drop) {
        this.dropHand = drop;
    }

    public void setDropHand() {
        this.setDropHand(true);
    }

    public void setDropOffhand(boolean drop) {
        this.dropOffhand = drop;
    }

    public void setDropOffhand() {
        this.setDropOffhand(true);
    }

    public void addDropSlot(int slot) {
        this.dropSlot.add(slot);
    }

    public int[] getDropSlots() {
        int[] tmp = new int[dropSlot.size()];
        int pos = 0;
        for (int x : dropSlot) {
            tmp[pos] = x;
            pos++;
        }
        return tmp;
    }

    public void removeDropSlot(int slot) {
        this.dropSlot.remove(slot);
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
        this.jumphigh = j;
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

    public void jump() {
        if (this.onGround) {
            this.motionY = 0.42 * jumphigh;
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

                LittleMonsterEntityDeathDropExpEvent expEvent = new LittleMonsterEntityDeathDropExpEvent(this, this.deathDropExp());
                Server.getInstance().getPluginManager().callEvent(expEvent);
                if (!expEvent.isCancelled()) {
                    int dropExp = expEvent.getDropExp();
                    int addition = 0;
                    // TODO 事件完成后移除这个兼容
                    if (LittleMonsterMainClass.hasRcRPG) {// 经验加成
                        try {
                            Method getPlayerAttr = Class.forName("RcRPG.AttrManager.PlayerAttr").getMethod("getPlayerAttr", Player.class);
                            Object manager = getPlayerAttr.invoke(null, damager);
                            float experienceGainMultiplier = manager.getClass().getField("experienceGainMultiplier").getFloat(manager);
                            if (experienceGainMultiplier > 0) {
                                addition = (int) (experienceGainMultiplier * dropExp);
                            }
                        } catch (Exception e) {
                            LittleMonsterMainClass.getInstance().getLogger().error("RcRPG经验加成获取失败", e);
                        }
                    }
                    String tipText = "经验 +" + dropExp;
                    if (addition > 0) {
                        tipText = "经验 +" + dropExp + "§a(" + addition + ")";
                        dropExp += addition;
                    }
                    if (dropExp > 0) {
                        ((Player) damager).addExperience(dropExp);// TODO:升级音效
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
}
