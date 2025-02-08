package com.smallaswater.littlemonster.entity.vanilla;

import cn.lanink.gamecore.utils.EntityUtils;
import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.custom.CustomEntity;
import cn.nukkit.entity.custom.EntityDefinition;
import cn.nukkit.entity.custom.EntityManager;
import cn.nukkit.entity.data.IntEntityData;
//import cn.nukkit.entity.data.Skin;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.SetEntityLinkPacket;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.IEntity;
import com.smallaswater.littlemonster.manager.BossBarManager;
import org.jetbrains.annotations.NotNull;

public class VanillaNPCCustomEntity extends VanillaNPC implements CustomEntity,IEntity {
    private static final EntityDefinition DEFAULT_DEFINITION = EntityDefinition.builder()
            .identifier("vanilla_npc_custom_entity")
            .spawnEgg(false)
            .implementation(VanillaNPCCustomEntity.class)
            .build();
    static {
        EntityManager.get().registerDefinition(DEFAULT_DEFINITION);
    }

    private EntityDefinition definition;


    public VanillaNPCCustomEntity(FullChunk chunk, CompoundTag nbt, @NotNull MonsterConfig config,boolean skip) {
        super(chunk, nbt, config, skip);
        this.setDefinition(config.getCustomEntityDefinition());
    }

    public void setDefinition(EntityDefinition definition) {
        this.definition = definition;
    }

    @Override
    public int getNetworkId() {
        return this.getEntityDefinition().getRuntimeId();
    }

    public void setIdentifier(String identifier) {
        this.definition = EntityDefinition.builder()
                .identifier(identifier)
                .spawnEgg(false)
                .implementation(VanillaNPCCustomEntity.class)
                .build();
    }

    /**
     * 获取原版实体定义
     *
     * @return 实体定义
     */
    @Override
    public EntityDefinition getEntityDefinition() {
        if (this.definition == null) {
            return DEFAULT_DEFINITION;
        }
        return this.definition;
    }

    public void setSkinId(int skinId) {
        this.namedTag.putInt("skinId", skinId);
        this.setDataProperty(
                new IntEntityData(EntityUtils.getEntityField("DATA_SKIN_ID", DATA_SKIN_ID),
                        this.namedTag.getInt("skinId")
                )
        );
    }

    public int getSkinId() {
        return this.namedTag.getInt("skinId");
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setDataProperty(
                new IntEntityData(EntityUtils.getEntityField("DATA_SKIN_ID", DATA_SKIN_ID),
                        this.namedTag.getInt("skinId")
                )
        );
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        this.level.addEntityMovement(this, x, y, z, yaw, pitch, headYaw);
    }

    @Override
    public void spawnTo(Player player) {
        if (!this.hasSpawned.containsKey(player.getLoaderId()) && this.chunk != null && player.usedChunks.containsKey(Level.chunkHash(this.chunk.getX(), this.chunk.getZ()))) {
            this.hasSpawned.put(player.getLoaderId(), player);
            player.dataPacket(createAddEntityPacket());
        }

        if (this.riding != null) {
            this.riding.spawnTo(player);

            SetEntityLinkPacket pk = new SetEntityLinkPacket();
            pk.vehicleUniqueId = this.riding.getId();
            pk.riderUniqueId = this.getId();
            pk.type = 1;
            pk.immediate = 1;

            player.dataPacket(pk);
        }
    }

    //基于原版实体目前不需要实现
    //@Override
    //public void setSkin(Skin skin) {
        //this.skin = skin;
    //}

    @Override
    public boolean isVanillaEntity() {
        return false;
    }

    protected int healTime = 0;

    public int healSettingTime;

    public int heal;

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
        //onHealthListener((int) Math.floor(getHealth()));

        if (this.getFollowTarget() != null && this.getFollowTarget() instanceof Player) {
            if (healTime >= healSettingTime &&
                    heal > 0 &&
                    !config.isUnFightHeal()) {
                healTime = 0;
                this.heal(heal);
            }
            if (config.isShowBossBar()) {
                if (targetOption(this.getFollowTarget(), this.distance(this.getFollowTarget()))) {
                    if (boss != null) {
                        BossBarManager.BossBarApi.removeBossBar(boss);
                        boss = null;
                    }
                    return false;
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
                if (healTime >= healSettingTime && heal > 0) {
                    healTime = 0;
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

        // 更新乘客
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
            if (this.getFollowTarget() != null) {
                lookAt(this.getFollowTarget());
            }
            //this.getLevel().addParticle(new HappyVillagerParticle(this.getPosition().add(0, getHeight())));
        }
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

}
