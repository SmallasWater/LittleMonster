package com.smallaswater.littlemonster.entity.spawner;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import com.smallaswater.littlemonster.LittleMasterMainClass;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.autospawn.AbstractEntitySpawner;
import com.smallaswater.littlemonster.entity.autospawn.SpawnResult;
import com.smallaswater.littlemonster.threads.runnables.AutoSpawnTask;


/**
 * @author SmallasWater
 * Create on 2021/12/6 15:38
 * Package com.smallaswater.littlemonster.entity.spawner
 */
public class LittleNpcSpawner extends AbstractEntitySpawner {

    private MonsterConfig config;

    public LittleNpcSpawner(MonsterConfig config,AutoSpawnTask spawnTask) {
        super(spawnTask);
        this.config = config;
        this.entityName = config.getName();
    }

    @Override
    public SpawnResult spawn(Player player, Position pos, Level level) {
        SpawnResult result = SpawnResult.OK;
        int blockId = level.getBlockIdAt((int)pos.x, (int)pos.y, (int)pos.z);
        int biomeId = level.getBiomeId((int)pos.x, (int)pos.z);
        if (Block.transparent[blockId]) {
            result = SpawnResult.WRONG_BLOCK;
        } else if (biomeId == 8) {
            result = SpawnResult.WRONG_BIOME;
        } else if (pos.y <= 255.0D && (!"nether".equals(level.getName()) || pos.y <= 127.0D) && pos.y >= 1.0D && blockId != 0) {
            config.spawn(pos,LittleMasterMainClass.getMasterMainClass().getConfig().getInt("autospawn."+getEntityName()+".liveTime",30));
        } else {
            result = SpawnResult.POSITION_MISMATCH;
        }

        return result;
    }

    @Override
    public String getEntityName() {
        return entityName;
    }
}
