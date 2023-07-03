package com.smallaswater.littlemonster.threads.runnables;

import cn.nukkit.scheduler.PluginTask;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.entity.LittleNpc;

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
        for (LittleNpc lt : routeEntitys.keySet()) {
            if (lt.route == null) {
                routeEntitys.remove(lt);
                continue;
            }
            if (System.currentTimeMillis() - routeEntitys.get(lt) > 50 * 100) {
                if (!lt.route.isSearching()) {
                    lt.route.research();
                    continue;
                }
                routeEntitys.put(lt, System.currentTimeMillis());
            }
        }
    }
}
