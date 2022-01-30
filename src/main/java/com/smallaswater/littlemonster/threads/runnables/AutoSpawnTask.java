package com.smallaswater.littlemonster.threads.runnables;

import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Config;
import com.smallaswater.littlemonster.LittleMasterMainClass;
import com.smallaswater.littlemonster.entity.autospawn.AbstractEntitySpawner;
import com.smallaswater.littlemonster.entity.spawner.LittleNpcSpawner;
import com.smallaswater.littlemonster.utils.Utils;


import java.util.*;

/**
 * @author MobPlugin
 */
public class AutoSpawnTask extends Thread{

    private Map<String, Integer> maxSpawns = new HashMap<>();
    private List<AbstractEntitySpawner> entitySpawners = new ArrayList<>();
    private Config pluginConfig;
    private LittleMasterMainClass plugin;

    public AutoSpawnTask(LittleMasterMainClass plugin) {
        this.pluginConfig = plugin.getConfig();
        this.plugin = plugin;
        this.prepareSpawnerClasses();
        this.prepareMaxSpawns();
    }

    private void prepareMaxSpawns() {
        int size  = 0;
        for(AbstractEntitySpawner spawner : entitySpawners){
            size = pluginConfig.getInt("autospawn."+spawner.getEntityName()+".maxCount");
            plugin.getLogger().info("加载自动生成 "+spawner.getEntityName()+" 模块 最大数量: "+size);
            maxSpawns.put(spawner.getEntityName(),size);
        }
    }

    private void prepareSpawnerClasses() {
        Object map = pluginConfig.get("autospawn");
        if(map instanceof Map){
            for(Object o: ((Map) map).keySet()){
                if(plugin.monsters.containsKey(o.toString())){
                    this.entitySpawners.add(new LittleNpcSpawner(plugin.monsters.get(o.toString()),this));
                }
            }
        }

    }

    @Override
    public void run() {
        if (this.plugin.getServer().getOnlinePlayers().size() > 0 && this.entitySpawners.size() > 0) {
            for (AbstractEntitySpawner spawner : this.entitySpawners) {
                spawner.spawn(this.plugin.getServer().getOnlinePlayers().values());
            }
        }

    }

    public boolean entitySpawnAllowed(Level level, String entityName, Vector3 pos) {
        int count = 0;
        for (Entity entity : level.getEntities()) {
            if (entity.isAlive() && entity.getName().equalsIgnoreCase(entityName)&& (new Vector3(pos.x, entity.y, pos.z)).distance(entity) < 100.0D) {
                ++count;
            }
        }

        return count < this.maxSpawns.getOrDefault(entityName, 0);
    }


    public int getRandomSafeXZCoord(int degree, int safeDegree, int correctionDegree) {
        int addX = Utils.rand(degree / 2 * -1, degree / 2);
        if (addX >= 0) {
            if (degree < safeDegree) {
                addX = safeDegree + Utils.rand(correctionDegree / 2 * -1, correctionDegree / 2);
            }
        } else if (degree > safeDegree) {
            addX = -safeDegree;
            addX += Utils.rand(correctionDegree / 2 * -1, correctionDegree / 2);
        }

        return addX;
    }

    public int getSafeYCoord(Level level, Position pos, int needDegree) {
        int x = (int)pos.x;
        int y = (int)pos.y;
        int z = (int)pos.z;
        int checkNeedDegree;
        int checkY;
        if (level.getBlockIdAt(x, y, z) == 0) {
            label51:
            while(true) {
                --y;
                if (y > 255) {
                    y = 256;
                    break;
                }

                if (y < 1) {
                    y = 0;
                    break;
                }

                if (level.getBlockIdAt(x, y, z) != 0) {
                    checkNeedDegree = needDegree;
                    checkY = y;

                    do {
                        ++checkY;
                        --checkNeedDegree;
                        if (checkY > 255 || checkY < 1 || level.getBlockIdAt(x, checkY, z) != 0) {
                            continue label51;
                        }
                    } while(checkNeedDegree > 0);

                    return y;
                }
            }
        } else {
            label67:
            while(true) {
                ++y;
                if (y > 255) {
                    y = 256;
                    break;
                }

                if (y < 1) {
                    y = 0;
                    break;
                }

                if (level.getBlockIdAt(x, y, z) != 0) {
                    checkNeedDegree = needDegree;
                    checkY = y;

                    do {
                        --checkY;
                        --checkNeedDegree;
                        if (checkY > 255 || checkY < 1 || level.getBlockIdAt(x, checkY, z) != 0) {
                            continue label67;
                        }
                    } while(checkNeedDegree > 0);

                    return y;
                }
            }
        }

        return y;
    }



}
