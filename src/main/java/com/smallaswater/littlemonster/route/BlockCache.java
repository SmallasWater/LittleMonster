package com.smallaswater.littlemonster.route;

import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Hash;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LT_Name
 */
public class BlockCache {

    public static final ConcurrentHashMap<Level, BlockCache> CACHES = new ConcurrentHashMap<>();

    public static BlockCache get(Level level) {
        if (!CACHES.containsKey(level)) {
            CACHES.put(level, new BlockCache(level));
        }
        return CACHES.get(level);
    }

    private final Level level;

    private final ConcurrentHashMap<Long, Block> blockCache1 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Block> blockCache2 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Block> blockCache3 = new ConcurrentHashMap<>();

    private boolean isCache1Ready = true;
    private boolean isCache2Ready = true;
    private boolean isCache3Ready = true;

    private int nowIndex = 1;

    public BlockCache(Level level) {
        this.level = level;
    }

    public Block getBlock(Vector3 vector3, boolean load) {
        return this.getBlock(vector3.getFloorX(), vector3.getFloorY(), vector3.getFloorZ(), load);
    }

    public Block getBlock(int x, int y, int z) {
        return this.getBlock(x, y, z, true);
    }

    public Block getBlock(int x, int y, int z, boolean load) {
        Long hash = Hash.hashBlock(x, y, z);
        return this.getBlockCache().computeIfAbsent(hash, (hash1) -> this.level.getBlock(x, y, z, load));
    }

    public ConcurrentHashMap<Long, Block> getBlockCache() {
        switch (this.nowIndex) {
            case 2:
                return this.blockCache2;
            case 3:
                return this.blockCache3;
            case 1:
            default:
                return this.blockCache1;
        }
    }

    public void switchCache() {
        //TODO 检查下一缓存是否准备就绪
        switch (this.nowIndex) {
            case 2:
                this.isCache3Ready = true;
                this.nowIndex = 3;
                this.isCache2Ready = false;
                break;
            case 3:
                this.isCache1Ready = true;
                this.nowIndex = 1;
                this.isCache3Ready = false;
                break;
            case 1:
            default:
                this.isCache2Ready = true;
                this.nowIndex = 2;
                this.isCache1Ready = false;
                break;
        }
    }

}
