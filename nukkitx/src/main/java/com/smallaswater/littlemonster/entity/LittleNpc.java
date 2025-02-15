package com.smallaswater.littlemonster.entity;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.BlockRedstone;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.entity.mob.EntityMob;
import cn.nukkit.entity.projectile.EntityArrow;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.entity.*;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.level.particle.HugeExplodeSeedParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.TextFormat;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.common.EntityTool;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.baselib.BaseEntityMove;
import com.smallaswater.littlemonster.events.entity.LittleMonsterEntityDeathDropExpEvent;
import com.smallaswater.littlemonster.handle.DamageHandle;
import com.smallaswater.littlemonster.items.BaseItem;
import com.smallaswater.littlemonster.items.DeathCommand;
import com.smallaswater.littlemonster.items.DropItem;
import com.smallaswater.littlemonster.manager.BossBarManager;
import com.smallaswater.littlemonster.skill.BaseSkillAreaManager;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import com.smallaswater.littlemonster.skill.defaultskill.AttributeHealthSkill;
import com.smallaswater.littlemonster.skill.defaultskill.MessageHealthSkill;
import com.smallaswater.littlemonster.skill.defaultskill.SummonHealthSkill;
import com.smallaswater.littlemonster.threads.runnables.RouteFinderRunnable;
import com.smallaswater.littlemonster.utils.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * @author SmallasWater
 * Create on 2021/6/28 8:36
 * Package com.smallaswater.littlemonster.entity
 */
public class LittleNpc extends BaseEntityMove implements IEntity {

    public static final String TAG = "LittleMonster";

    public String name;

    @Setter
    @Getter
    private int liveTime = -1;

    public String spawnPos = null;

    //TODO 唯一标识
    public UUID uuid;

    public LittleNpc(FullChunk chunk, CompoundTag nbt, MonsterConfig config) {
        super(chunk, nbt, config);
        this.name = config.getName();
        this.setNameTagAlwaysVisible();
        this.setNameTagVisible();
        this.loadSkill();
        this.setDataFlag(Entity.DATA_LEAD_HOLDER_EID, -1);
        this.setMaxHealth(config.getHealth());
        this.setHealth(config.getHealth());
        this.namedTag.putString(TAG, name);
        this.destinationDeviate = Math.max(1.5, config.getAttackDistance() * 0.8);
        if (this.route != null) {
            this.route.setDestinationDeviate(this.destinationDeviate);
        }
        uuid = UUID.randomUUID();
        RouteFinderRunnable.addRoute(this);
    }

    private void disCommand(String cmd) {
        disCommand(cmd, null, null);
    }

    private void disCommand(String cmd, String target, String name) {
        if (target != null) {
            Server.getInstance().getCommandMap().dispatch(
                    new EntityCommandSender(getName()), cmd.replace(target, name));
        } else {
            Server.getInstance().getCommandMap().dispatch(new EntityCommandSender(getName()), cmd);
        }
    }

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
            Server.getInstance().broadcastMessage(TextFormat.colorize('&', deathMessage.replace("{name}", name)
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
                            .replace("{name}", name)
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
    public String getName() {
        return name;
    }

    private Player boss = null;

    private int age = 0;

    private int cacheAge = 0;

    @Override
    public void onUpdata() {
        if (cacheAge >= 20) {
            age++;
            cacheAge = 0;
        } else {
            cacheAge++;
        }
        if (liveTime != -1 && age >= liveTime) {
            this.getLevel().addParticleEffect(this, ParticleEffect.BASIC_SMOKE);
            this.close();
            return;
        }
        // 技能召唤的
        if (this.deathFollowMaster && masterHuman != null) {
            if (masterHuman.isClosed()) {
                this.close();
                return;
            }
        }
        if (config == null) {
            this.close();
            return;
        }
        this.setNameTag(config.getTag()
                .replace("{名称}", name)
                .replace("{血量}", getHealth() + "")
                .replace("{最大血量}", getMaxHealth() + ""));
        onHealthListener((int) Math.floor(getHealth()));
    }

    @Override
    public void reset() {
        super.reset();
        if (config != null) {
            config.npcSetting(this);
        }
    }

    //受到攻击
    @Override
    public void onAttack(EntityDamageEvent sure) {
        if (isImmobile() && !config.isImmobile() && !LittleMonsterMainClass.hasRcRPG) {
            sure.setCancelled();
        }
        if (sure instanceof EntityDamageByEntityEvent) {
            if (config.isPassiveAttackEntity()) {
                if (((EntityDamageByEntityEvent) sure).getDamager() instanceof Player) {
                    Player player = (Player) ((EntityDamageByEntityEvent) sure).getDamager();
                    if (!targetOption(player, this.distance(player))) {
                        this.getTargetWeighted(player).setReason(TargetWeighted.REASON_PASSIVE_ATTACK_ENTITY);
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
                        this.getTargetWeighted((EntityCreature) damager).setReason(TargetWeighted.REASON_PASSIVE_ATTACK_ENTITY);
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
        }
        this.level.addParticle(new DestroyBlockParticle(this, new BlockRedstone()));
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        boolean attack = super.attack(source);
        //获取最终伤害 计算目标权重
        if (!source.isCancelled() && source instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent) source;
            TargetWeighted targetWeighted = this.getTargetWeighted(this);
            targetWeighted.setCauseDamage(targetWeighted.getCauseDamage() + entityDamageByEntityEvent.getFinalDamage());
        }

        return attack;
    }

    private void toDamageEntity(EntityDamageByEntityEvent sure) {
        if (config.isPassiveAttackEntity()) {
            if (!config.isAttackHostileEntity()) {
                if (sure.getDamager() instanceof EntityMob) {
                    return;
                }
            }
            if (sure.getDamager() instanceof LittleNpc) {
                if (!Utils.canAttackNpc(this, (LittleNpc) sure.getDamager(), true)) {
                    return;
                }
            }
            if (!targetOption(sure.getDamager(), distance(sure.getDamager())) && sure.getDamager() instanceof EntityCreature) {
                this.getTargetWeighted((EntityCreature) sure.getDamager()).setReason(TargetWeighted.REASON_PASSIVE_ATTACK_ENTITY);
                //setFollowTarget(sure.getDamager());
            }
        }
    }

    @Override
    public void close() {
        if (inventory != null && inventory.getViewers() != null) {
            inventory.getViewers().clear();
        }
        if (this.route != null) {
            this.route.interrupt();
        }
        RouteFinderRunnable.routeEntitys.remove(this);

        super.close();
        if (boss != null) {
            BossBarManager.BossBarApi.removeBossBar(boss);
            boss = null;
        }
        if (config != null) {
            if (config.isDisplayDamage() && this.handle != null) {
                handle.display();
            }
        }
        this.handle = null;
        RouteFinderRunnable.removeRoute(this);
        this.route = null;
    }

    @Override
    public void saveNBT() {
        // ignore
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LittleNpc)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        LittleNpc littleNpc = (LittleNpc) o;

        if (!name.equals(littleNpc.name)) {
            return false;
        }
        return uuid.equals(littleNpc.uuid);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        if (name != null) {
            result = 31 * result + name.hashCode();
        }
        if (uuid != null) {
            result = 31 * result + uuid.hashCode();
        }
        return result;
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
    public boolean isVanillaEntity() {
        return false;
    }

    @Override
    public Entity getEntity() {
        return this;
    }

    @Override
    public void setSkinByIEntity(Skin skin) {
        this.setSkin(skin);
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
