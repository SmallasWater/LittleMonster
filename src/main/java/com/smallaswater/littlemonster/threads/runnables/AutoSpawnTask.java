package com.smallaswater.littlemonster.threads.runnables;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.utils.Config;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.entity.autospawn.AbstractEntitySpawner;
import com.smallaswater.littlemonster.entity.spawner.LittleNpcSpawner;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author MobPlugin
 */
public class AutoSpawnTask extends Thread {

    private final Map<String, Integer> maxSpawns = new HashMap<>();

    private final List<AbstractEntitySpawner> entitySpawners = new ArrayList<>();

    private final LittleMonsterMainClass plugin;

    private final Config pluginConfig;

    public AutoSpawnTask(LittleMonsterMainClass plugin) {
        this.plugin = plugin;
        this.pluginConfig = plugin.getConfig();
        this.prepareSpawnerClasses();
        this.prepareMaxSpawns();
    }

    /**
     * 加载自动生成的实体
     */
    private void prepareSpawnerClasses() {
        Object map = pluginConfig.get("autospawn");
        if (map instanceof Map) {
            for (Object o : ((Map) map).keySet()) {
                if (plugin.monsters.containsKey(o.toString())) {
                    this.entitySpawners.add(new LittleNpcSpawner(plugin.monsters.get(o.toString()), this));
                }
            }
        }
    }

    /**
     * 加载最大生成数量限制
     */
    private void prepareMaxSpawns() {
        for (AbstractEntitySpawner spawner : this.entitySpawners) {
            int maxCount = pluginConfig.getInt("autospawn." + spawner.getEntityName() + ".maxCount");
            plugin.getLogger().info("加载自动生成 " + spawner.getEntityName() + " 模块 最大数量: " + maxCount);
            maxSpawns.put(spawner.getEntityName(), maxCount);
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
            if (entity.isAlive() && entity.getName().equalsIgnoreCase(entityName) && (new Vector3(pos.x, entity.y, pos.z)).distance(entity) < 100.0D) {
                ++count;
            }
        }

        return count < this.maxSpawns.getOrDefault(entityName, 0);
    }


    public int getRandomSafeXZCoord(int degree, int safeDegree, int correctionDegree) {
        int addX = nukkitcoders.mobplugin.utils.Utils.rand((degree >> 1) * -1, degree >> 1);
        if (addX >= 0) {
            if (degree < safeDegree) {
                addX = safeDegree;
                addX += nukkitcoders.mobplugin.utils.Utils.rand((correctionDegree >> 1) * -1, correctionDegree >> 1);
            }
        } else {
            if (degree > safeDegree) {
                addX = -safeDegree;
                addX += Utils.rand((correctionDegree >> 1) * -1, correctionDegree >> 1);
            }
        }

        return addX;
    }

    public int getSafeYCoord(Level level, Position pos) {
        int x = (int) pos.x;
        int y = (int) pos.y;
        int z = (int) pos.z;

        if (level.getBlockIdAt(x, y, z) == Block.AIR) {
            while (true) {
                y--;
                if (y > 255) {
                    y = 256;
                    break;
                }
                if (y < 1) {
                    y = 0;
                    break;
                }
                if (level.getBlockIdAt(x, y, z) != Block.AIR) {
                    int checkNeedDegree = 3;
                    int checkY = y;
                    while (true) {
                        checkY++;
                        checkNeedDegree--;
                        if (checkY > 255 || level.getBlockIdAt(x, checkY, z) != Block.AIR) {
                            break;
                        }

                        if (checkNeedDegree <= 0) {
                            return y;
                        }
                    }
                }
            }
        } else {
            while (true) {
                y++;
                if (y > 255) {
                    y = 256;
                    break;
                }

                if (y < 1) {
                    y = 0;
                    break;
                }

                if (level.getBlockIdAt(x, y, z) != Block.AIR) {
                    int checkNeedDegree = 3;
                    int checkY = y;
                    while (true) {
                        checkY--;
                        checkNeedDegree--;
                        if (checkY < 1 || level.getBlockIdAt(x, checkY, z) != Block.AIR) {
                            break;
                        }

                        if (checkNeedDegree <= 0) {
                            return y;
                        }
                    }
                }
            }
        }
        return y;
    }

}
