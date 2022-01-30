package com.smallaswater.littlemonster.manager;

import cn.nukkit.Player;
import com.smallaswater.littlemonster.handle.TimeHandle;

import java.util.LinkedHashMap;

/**
 * @author SmallasWater
 * Create on 2021/6/2 13:05
 * Package com.smallaswater.customvip.manager
 */
public class TimerHandleManager {

    public static LinkedHashMap<Player, TimeHandle> PLAYER_TIMER = new LinkedHashMap<>();


    public static TimeHandle getTimeHandle(Player player){
        if(!PLAYER_TIMER.containsKey(player)){
            PLAYER_TIMER.put(player,new TimeHandle(player));
        }
        return PLAYER_TIMER.get(player);
    }
}
