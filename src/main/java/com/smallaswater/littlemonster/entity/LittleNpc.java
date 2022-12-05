package com.smallaswater.littlemonster.entity;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.BlockRedstone;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
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
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.baselib.BaseEntityMove;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author SmallasWater
 * Create on 2021/6/28 8:36
 * Package com.smallaswater.littlemonster.entity
 */
public class LittleNpc extends BaseEntityMove {

    public static final String TAG = "LittleMonster";

    public String name;

    @Setter
    @Getter
    private int liveTime = -1;

    public String spawnPos = null;

    public int heal;

    public int healSettingTime;

    private ArrayList<Integer> healthList = new ArrayList<>();

    public DamageHandle handle = new DamageHandle();

    //TODO 唯一标识
    public UUID uuid;

    public LittleNpc(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.close();
    }

    public LittleNpc(FullChunk chunk, CompoundTag nbt, @NotNull MonsterConfig config){
        super(chunk, nbt);
        this.config = config;
        this.name = config.getName();
        this.setNameTagAlwaysVisible();
        this.setNameTagVisible();
        this.loadSkill();
        this.setDataFlag(Entity.DATA_LEAD_HOLDER_EID,-1);
        this.setHealth(config.getHealth());
        this.setMaxHealth(config.getHealth());
        this.namedTag.putString(TAG,name);
        this.destinationDeviate = Math.max(1.5, config.getAttackDistance() * 0.8);
        if (this.route != null) {
            this.route.setDestinationDeviate(this.destinationDeviate);
        }
        uuid = UUID.randomUUID();
        RouteFinderRunnable.addRoute(this);
    }

    private Player getDamageMax(){
        double max = 0;
        Player p = null;
        for(Map.Entry<String, Double> player: handle.playerDamageList.entrySet()){
            if(player.getValue() > max){
                if(Server.getInstance().getPlayer(player.getKey()) != null){
                    p = Server.getInstance().getPlayer(player.getKey());
                }
                max = player.getValue();
            }
        }
        return p;
    }

    private void disCommand(String cmd){
        disCommand(cmd,null,null);
    }

    private void disCommand(String cmd,String target,String name){
        if(target != null) {
            Server.getInstance().getCommandMap().dispatch(
                    new EntityCommandSender(getName()), cmd.replace(target, name));
        }else{
            Server.getInstance().getCommandMap().dispatch(new EntityCommandSender(getName()),cmd);
        }
    }

    public void onDeath(EntityDeathEvent event){
        Entity damager = null;
        EntityDamageEvent d = event.getEntity().getLastDamageCause();
        if(d instanceof EntityDamageByEntityEvent){
            damager = ((EntityDamageByEntityEvent) d).getDamager();
        }
        LinkedList<Item> items = new LinkedList<>();
        for(DeathCommand command:getConfig().getDeathCommand()){
            if(command.getRound() >= Utils.rand(1,100)){
                String cmd = command.getCmd();
                cmd = cmd.replace("{x}",String.format("%.2f",getX()))
                          .replace("{y}",String.format("%.2f",getY()))
                          .replace("{z}",String.format("%.2f",getZ()))
                          .replace("{level}",getLevel().getFolderName());
                if(cmd.contains("@"+ BaseItem.TARGETALL)){
                    for(Player player: getDamagePlayers()){
                        disCommand(cmd,"@"+BaseItem.TARGETALL,player.getName());
                    }

                }else{
                    if(cmd.contains("@"+BaseItem.TARGET)){
                        if(damager instanceof  Player){
                            cmd = cmd.replace("@"+BaseItem.TARGET,damager.getName());
                        }
                    }
                    if(cmd.contains("@"+BaseItem.DAMAGE)){
                        Player player = getDamageMax();
                        if(player != null){
                            cmd = cmd.replace("@"+BaseItem.DAMAGE,player.getName());
                        }
                    }
                    disCommand(cmd);
                }
            }
        }
        for(DropItem key: getConfig().getDeathItem()){
            if(key.getRound() >= Utils.rand(1,100)){
                items.add(key.getItem());
            }
        }
        event.setDrops(items.toArray(new Item[0]));

        String deathMessage = getConfig().getConfig().getString("公告.死亡.信息","&e[ &bBOSS &e] {name} 在坐标: x: {x} y: {y} z: {z} 处死亡");
        if(getConfig().getConfig().getBoolean("公告.死亡.是否提示",true)){
            Server.getInstance().broadcastMessage(TextFormat.colorize('&',deathMessage.replace("{name}",name)
                    .replace("{x}",getFloorX()+"")
                    .replace("{y}",getFloorY()+"")
                    .replace("{z}",getFloorZ()+"")
                    .replace("{level}",getLevel().getFolderName()+"")));
        }
        if(damager != null){
            if(damager instanceof Player) {
                String killMessage = getConfig().getConfig().getString("公告.击杀.信息", "&e[ &bBOSS提醒 &e] &d{name} 被 {player} 击杀");
                if (getConfig().getConfig().getBoolean("公告.击杀.是否提示", true)) {
                    Server.getInstance().broadcastMessage(TextFormat.colorize('&', killMessage.replace("{name}", name)

                            .replace("{player}", damager.getName() + "")));
                }
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void onClose() {
        if(boss != null){
            BossBarManager.BossBarApi.removeBossBar(boss);
            boss = null;
        }
        if(config != null){
            if(config.isDisplayDamage() && this.handle != null){
                handle.display();
            }
        }

        //如果在类实例化时调用onClose方法 这些变量可能为null
        if (this.targetWeightedMap != null) {
            this.targetWeightedMap.clear();
        }
        if (this.skillManagers != null) {
            this.skillManagers.clear();
        }
        if (this.healthList != null) {
            this.healthList.clear();
        }
        this.handle = null;
        this.route = null;
    }

    private ArrayList<Player> getDamagePlayers(){
        ArrayList<Player> players = new ArrayList<>();
        Player player;
        for(String name: handle.playerDamageList.keySet()){
            player = Server.getInstance().getPlayer(name);
            if(player != null){
                players.add(player);
            }
        }
        return players;
    }

    private void loadSkill(){
        BaseSkillManager baseSkillManager;
        for(BaseSkillManager skillManager: config.getSkillManagers()){
            baseSkillManager = skillManager.clone();
            baseSkillManager.setMaster(this);
            skillManagers.add(baseSkillManager);
        }
    }

    private ArrayList<BaseSkillManager> getHealthSkill(int health){

        ArrayList<BaseSkillManager> skillManagers = new ArrayList<>();
        for(BaseSkillManager skillManager:this.skillManagers){
            if(skillManager.health >= health){
                if(!healthList.contains(skillManager.health)) {
                    skillManagers.add(skillManager);
                    healthList.add(skillManager.health);
                }
            }
        }
        return skillManagers;
    }

    private Player boss = null;

    private void onHealthListener(int health){
        ArrayList<BaseSkillManager> skillAreaManagers = getHealthSkill(health);
        if(skillAreaManagers.size() > 0) {
            for (BaseSkillManager skillManager : skillAreaManagers) {
                if (skillManager instanceof BaseSkillAreaManager) {
                    if (this.getFollowTarget() != null) {
                        if (!targetOption(this.getFollowTarget(), distance(this.getFollowTarget()))) {

                            if (skillManager.mode == 1) {
                                skillManager.display(Utils.getAroundPlayers(this, config.getArea(),true,true,true).toArray(new Entity[0]));
                            } else {
                                if (this.getFollowTarget() instanceof Player) {
                                    skillManager.display(this.getFollowTarget());
                                }
                            }
                        }
                    }
                } else {
                    if (skillManager instanceof AttributeHealthSkill) {
                        skillManager.display((Player) null);
                    }
                    if(skillManager instanceof SummonHealthSkill){
                        skillManager.display(this);
                    }
                    if (skillManager instanceof MessageHealthSkill) {
                        skillManager.display(getDamagePlayers().toArray(new Player[0]));
                    }
                }
            }
        }
    }

    private int age = 0;

    private int cacheAge = 0;

    @Override
    public void onUpdata() {
        if(cacheAge >= 20){
            age++;
            cacheAge = 0;
        }else{
            cacheAge++;
        }
        if(liveTime != -1 && age >= liveTime){
            this.getLevel().addParticleEffect(this,
                    ParticleEffect.BASIC_SMOKE);
            this.close();
            return;
        }
        // 技能召唤的
        if(isToDeath && masterHuman != null){
            if(masterHuman.isClosed()){
                this.close();
                return;
            }
        }
        if(config == null){
            this.close();
            return;
        }
        this.setNameTag(config.getTag()
                .replace("{名称}",name)
                .replace("{血量}",getHealth()+"")
                .replace("{最大血量}",getMaxHealth()+""));
        onHealthListener((int) Math.floor(getHealth()));
        if(this.getFollowTarget() != null && this.getFollowTarget() instanceof Player){
            if(healTime >= healSettingTime &&
                    heal > 0 &&
                    !config.isUnFightHeal()){
                healTime = 0;
                this.heal(heal);
            }
            if(targetOption(this.getFollowTarget(), this.distance(this.getFollowTarget()))) {
                if(boss != null){
                    BossBarManager.BossBarApi.removeBossBar(boss);
                    boss = null;
                }
                return;
            }
            if(this.getFollowTarget() instanceof Player){
                boss = (Player) this.getFollowTarget();
                if(!BossBarManager.BossBarApi.hasCreate((Player) this.getFollowTarget(),getId())) {
                    BossBarManager.BossBarApi.createBossBar((Player) this.getFollowTarget(), getId());
                }
                BossBarManager.BossBarApi.showBoss((Player) getFollowTarget(),
                        getNameTag(),
                        getHealth(),
                        getMaxHealth());
            }
        }else{
            if(getFollowTarget() == null || !config.isUnFightHeal()){
                if(healTime >= healSettingTime && heal > 0){
                    healTime = 0;
                    this.heal(heal);
                }
            }
            if(boss != null){
                BossBarManager.BossBarApi.removeBossBar(boss);
                boss = null;
            }
        }
    }

    @Override
    public void heal(float amount) {
        if (getHealth() < getMaxHealth()) {
            healthList.removeIf(i -> getHealth() + amount >= i);
            if (getHealth() + amount >= getMaxHealth()) {
                reset();
            }
        }
        this.heal(new EntityRegainHealthEvent(this, amount, 0));
    }

    public void reset(){
        handle = new DamageHandle();
        healthList = new ArrayList<>();
        if(config != null) {
            config.npcSetting(this);
        }
    }

    //受到攻击

    @Override
    public void onAttack(EntityDamageEvent sure) {
        if(this.damageDelay > config.getInvincibleTime()) {
            if (sure.getAttackCooldown() > this.config.getInvincibleTime()) {
                sure.setAttackCooldown(this.config.getInvincibleTime());
            }
            if(isImmobile() && !config.isImmobile()){
                sure.setCancelled();
            }
            if(!config.isKnock()){
                if(sure instanceof EntityDamageByEntityEvent){
                    ((EntityDamageByEntityEvent) sure).setKnockBack(0);
                }
            }
            this.damageDelay = 0;
            this.level.addParticle(new DestroyBlockParticle(this,new BlockRedstone()));
            if(sure instanceof EntityDamageByEntityEvent){
                if (config.isPassiveAttackEntity()) {
                    if (((EntityDamageByEntityEvent) sure).getDamager() instanceof Player) {
                        Player player = (Player) ((EntityDamageByEntityEvent) sure).getDamager();
                        if (!targetOption(player, this.distance(player))) {
                            this.getTargetWeighted(player).setReason(TargetWeighted.REASON_PASSIVE_ATTACK_ENTITY);
                            //setFollowTarget(player);
                        }

                    } else {
                        Entity damager = ((EntityDamageByEntityEvent) sure).getDamager();
                        if (!config.isAttackHostileEntity()) {
                            if (damager instanceof EntityMob) {
                                return;
                            }
                        }
                        if (damager instanceof LittleNpc) {
                            if (!Utils.canAttackNpc(this, (LittleNpc) damager,true)) {
                                return;
                            }
                        }
                        if (!targetOption(damager, distance(damager)) && damager instanceof EntityCreature) {
                            this.getTargetWeighted((EntityCreature) damager).setReason(TargetWeighted.REASON_PASSIVE_ATTACK_ENTITY);
                        }
                    }
                }
                if (((EntityDamageByEntityEvent) sure).getDamager() instanceof Player) {
                    Player player = (Player) ((EntityDamageByEntityEvent) sure).getDamager();
                    this.handle.add(player.getName(), sure.getFinalDamage());
                }
            }
        }else{
            sure.setCancelled();
        }
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        boolean attack = super.attack(source);
        //获取最终伤害
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
                if (!Utils.canAttackNpc(this, (LittleNpc) sure.getDamager(),true)) {
                    return;
                }
            }
            if (!targetOption(sure.getDamager(), distance(sure.getDamager())) && sure.getDamager() instanceof EntityCreature) {
                this.getTargetWeighted((EntityCreature) sure.getDamager()).setReason(TargetWeighted.REASON_PASSIVE_ATTACK_ENTITY);
                //setFollowTarget(sure.getDamager());
            }
        }
    }

    //攻击玩家~

    @Override
    public void attackEntity(EntityCreature entity){
        if (this.attackDelay > attackSleepTime) {
            this.attackDelay = 0;
            switch (attactMode){
                case 1:
                    //群体
                    if(entity.distance(this) <= distanceLine) {
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
                    }
                    break;
                case 2:
                    if(entity.distance(this) <= distanceLine) {
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
                                projectile.spawnToAll();
                                ((EntityArrow) projectile).setPickupMode(EntityArrow.PICKUP_NONE);
                                this.level.addSound(this, Sound.RANDOM_BOW);
                            }
                        }
                        EntityEventPacket pk = new EntityEventPacket();
                        pk.eid = this.getId();
                        pk.event = EntityEventPacket.ARM_SWING;
                        Server.broadcastPacket(this.getViewers().values(), pk);
                        waitTime = 0;
                    }
                    return;
                case 3: //触发EntityInteractEvent
                    if(entity.distance(this) <= seeSize) {
//                        if (!hasBlockInLine(entity)) {
                            EntityInteractEvent event = new EntityInteractEvent(this, entity.getPosition().add(0.5, entity.getEyeHeight(), 0.5).getLevelBlock());
                            Server.getInstance().getPluginManager().callEvent(event);
                            waitTime = 0;
//                        }
                    }
                    break;
                case 0:
                default:
                    HashMap<EntityDamageEvent.DamageModifier, Float> damage = new LinkedHashMap<>();
                    damage.put(EntityDamageEvent.DamageModifier.BASE, getDamage());
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
                        Item[] var5 = ((Player)entity).getInventory().getArmorContents();
                        for (Item i : var5) {
                            points += armorValues.getOrDefault(i.getId(), 0.0F);
                        }

                        damage.put(EntityDamageEvent.DamageModifier.ARMOR, (float)((double) damage.getOrDefault(EntityDamageEvent.DamageModifier.ARMOR, 0.0F) - Math.floor((double)(damage.getOrDefault(EntityDamageEvent.DamageModifier.BASE, 1.0F) * points) * 0.04D)));
                    }

                    entity.attack(new EntityDamageByEntityEvent(this, entity, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage,(float) config.getKnockBack()));
                    break;
            }
            for(Effect effect: config.getEffects()){
                entity.addEffect(effect);
            }
            EntityEventPacket pk = new EntityEventPacket();
            pk.eid = this.getId();
            pk.event = EntityEventPacket.ARM_SWING;
            Server.broadcastPacket(this.getViewers().values(), pk);
        }
    }

    @Override
    public void close() {
        if(inventory != null && inventory.getViewers() != null){
            inventory.getViewers().clear();
        }
        super.close();
        if (this.route != null) {
            this.route.interrupt();
        }
        RouteFinderRunnable.routeEntitys.remove(this);

        this.onClose();
    }

    @Override
    public void saveNBT() {}

    @Override
    public float getDamage() {
        return (float) damage;
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
        result = 31 * result + name.hashCode();
        result = 31 * result + uuid.hashCode();
        return result;
    }
}
