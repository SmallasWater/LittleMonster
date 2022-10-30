package com.smallaswater.littlemonster.threads.runnables;

import cn.nukkit.block.Block;
import com.smallaswater.littlemonster.route.BlockCache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 方块缓存清理任务
 *
 * @author LT_Name
 */
public class BlockCacheClearTask extends BasePluginThreadTask {

    public BlockCacheClearTask() {
        super(6000);
    }

    @Override
    public boolean scheduler() {
        for (BlockCache blockCache : BlockCache.CACHES.values()) {
            ConcurrentHashMap<Long, Block> cache = blockCache.getBlockCache();
            blockCache.switchCache();
            cache.clear();
        }
        return true;
    }

}
