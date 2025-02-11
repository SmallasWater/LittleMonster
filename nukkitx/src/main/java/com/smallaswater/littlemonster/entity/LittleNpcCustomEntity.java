package com.smallaswater.littlemonster.entity;

import cn.lanink.gamecore.utils.EntityUtils;
import cn.nukkit.Player;
import cn.nukkit.entity.custom.CustomEntity;
import cn.nukkit.entity.custom.EntityDefinition;
import cn.nukkit.entity.custom.EntityManager;
import cn.nukkit.entity.data.IntEntityData;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.SetEntityLinkPacket;
import com.smallaswater.littlemonster.config.MonsterConfig;
import org.jetbrains.annotations.NotNull;

/**
 * @author LT_Name
 */
public class LittleNpcCustomEntity extends LittleNpc implements CustomEntity, IEntity {

    private static final EntityDefinition DEFAULT_DEFINITION = EntityDefinition.builder()
            .identifier("little_npc_custom_entity")
            .spawnEgg(false)
            .implementation(LittleNpcCustomEntity.class)
            .build();

    static {
        EntityManager.get().registerDefinition(DEFAULT_DEFINITION);
    }

    private EntityDefinition definition;

    public LittleNpcCustomEntity(FullChunk chunk, CompoundTag nbt, MonsterConfig config) {
        super(chunk, nbt, config);
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
                .implementation(LittleNpcCustomEntity.class)
                .build();
    }

    /**
     * 获取实体定义
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
    protected void initEntity() {
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

    @Override
    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    @Override
    public boolean isVanillaEntity() {
        return false;
    }
}
