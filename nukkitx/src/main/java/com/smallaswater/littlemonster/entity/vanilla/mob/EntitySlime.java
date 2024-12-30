package com.smallaswater.littlemonster.entity.vanilla.mob;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.vanilla.VanillaNPC;

public class EntitySlime extends VanillaNPC {

    public static final int NETWORK_ID = 37;

    public static final int SIZE_SMALL = 1;
    public static final int SIZE_MEDIUM = 2;
    public static final int SIZE_BIG = 4;

    protected int size = SIZE_SMALL;

    public EntitySlime(FullChunk chunk, CompoundTag nbt, MonsterConfig config) {
        super(chunk, nbt, config, true);// 过程种会执行 initEntity()
        this.setMaxHealth(config.getHealth());
        this.setHealth(config.getHealth());
        this.setNameTagVisible(true);
        this.setNameTagAlwaysVisible(true);
        vanillaNPC = this;
        this.dataProperties.putFloat(DATA_BOUNDING_BOX_HEIGHT, this.getHeight());
        this.dataProperties.putFloat(DATA_BOUNDING_BOX_WIDTH, this.getWidth());
        //this.setHeight(getHeight());
        //this.setWidth(getWidth());
        halfWidth = this.getWidth() / 2;
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return size * 0.51f;
    }

    @Override
    public float getHeight() {
        return size * 0.51f;
    }

    @Override
    public float getLength() {
        return size * 0.51f;
    }

    @Override
    public void initEntity() {
        super.initEntity();

        if (this.namedTag.contains("Size")) {
            this.size = this.namedTag.getInt("Size");
        } else {
            this.size = SIZE_MEDIUM;// TODO: Utils.rand(1, 3);
            if (this.size == 3) {
                this.size = 4;
            }
        }

        //this.setScale(0.51f + size * 0.51f);
    }
//
//    @Override
//    public boolean entityBaseTick(int tickDiff) {
//        if (this.closed) {
//            return false;
//        }
//        return super.entityBaseTick(tickDiff);
//    }

    @Override
    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putInt("Size", this.size);
    }

    public int getSlimeSize() {
        return this.size;
    }
}