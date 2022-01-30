package com.smallaswater.littlemonster.threads.runnables;


import cn.nukkit.Player;
import com.smallaswater.littlemonster.handle.TimeHandle;
import com.smallaswater.littlemonster.manager.TimerHandleManager;

import java.util.Map;


/**
 * @author SmallasWater
 * Create on 2021/5/30 22:03
 * Package com.smallaswater.customvip.task
 */
public class TimmerRunnable extends BasePluginThreadTask {

    @Override
    public boolean scheduler() {
        for (Map.Entry<Player, TimeHandle> timeHandleEntry : TimerHandleManager.PLAYER_TIMER.entrySet()) {
            timeHandleEntry.getValue().loading();
        }
        return true;
    }
}
