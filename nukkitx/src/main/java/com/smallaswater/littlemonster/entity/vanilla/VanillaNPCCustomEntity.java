package com.smallaswater.littlemonster.entity.vanilla;

import cn.lanink.gamecore.utils.EntityUtils;
import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.custom.CustomEntity;
import cn.nukkit.entity.custom.EntityDefinition;
import cn.nukkit.entity.custom.EntityManager;
import cn.nukkit.entity.data.IntEntityData;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.SetEntityLinkPacket;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.IEntity;
import org.jetbrains.annotations.NotNull;

/**
 * 相较于 LittleNpcCustomEntity 此类可以显示正确的 vanilla entity 碰撞箱
 */
public class VanillaNPCCustomEntity extends VanillaNPC implements CustomEntity, IEntity {
    private static final EntityDefinition DEFAULT_DEFINITION = EntityDefinition.builder()
            .identifier("vanilla_npc_custom_entity")
            .spawnEgg(false)
            .implementation(VanillaNPCCustomEntity.class)
            .build();

    static {
        EntityManager.get().registerDefinition(DEFAULT_DEFINITION);
    }

    private EntityDefinition definition;


    public VanillaNPCCustomEntity(FullChunk chunk, CompoundTag nbt, @NotNull MonsterConfig config, boolean skip) {
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

    @Override
    public boolean onUpdate(int currentTick) {
        // 更新乘客
        try {
            for (Entity entity : this.getPassengers()) {
                if (entity.distance(this) > 3) {
                    this.setEntityRideOff(entity);
                }
            }
        } catch (java.util.ConcurrentModificationException ignored) {
        }

        //处理骑乘
        this.updatePassengers();

        if (currentTick % 10 == 0) {
            if (this.getFollowTarget() != null) {
                lookAt(this.getFollowTarget());
            }
//            this.getLevel().addParticle(new HappyVillagerParticle(this.getPosition().add(0, getHeight())));
        }
        return super.onUpdate(currentTick);
    }

}
