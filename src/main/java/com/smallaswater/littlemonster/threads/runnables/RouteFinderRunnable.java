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

    public static void addRoute(LittleNpc e){
        routeEntitys.put(e,System.currentTimeMillis());
    }


    @Override
    public void onRun(int i) {
        for (LittleNpc lt: routeEntitys.keySet()) {
            if(System.currentTimeMillis() - routeEntitys.get(lt) > 50 * 100){
                if (!lt.route.isSearching()) {
                    lt.route.research(true);
                    continue;
                }
                routeEntitys.put(lt,System.currentTimeMillis());
            }

        }
    }
}
