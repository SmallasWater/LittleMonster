package com.smallaswater.littlemonster.threads.runnables;

import cn.nukkit.scheduler.PluginTask;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.entity.LittleNpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author SmallasWater
 * @date 2022/12/5
 */
public class RouteFinderRunnable extends PluginTask<LittleMonsterMainClass> {
    public static ConcurrentHashMap<LittleNpc, Long> routeEntitys = new ConcurrentHashMap<>();

    public RouteFinderRunnable(LittleMonsterMainClass littleMonsterMainClass) {
        super(littleMonsterMainClass);
    }

    public static void addRoute(LittleNpc e) {
        routeEntitys.put(e, System.currentTimeMillis());
    }

    public static void removeRoute(LittleNpc e) {
        routeEntitys.remove(e);
    }

    @Override
    public void onRun(int i) {
        for (Map.Entry<LittleNpc, Long> entry : routeEntitys.entrySet()) {
            if (entry.getKey().route == null) {
                routeEntitys.remove(entry.getKey());
                continue;
            }
            if (System.currentTimeMillis() - entry.getValue() > 50 * 100) {
                if (!entry.getKey().route.isSearching()) {
                    entry.getKey().route.research();
                    continue;
                }
                routeEntitys.put(entry.getKey(), System.currentTimeMillis());
            }
        }
    }
}
