package com.smallaswater.littlemonster.manager;

import cn.nukkit.Player;
import com.smallaswater.littlemonster.handle.KeyHandle;
import com.smallaswater.littlemonster.handle.TimeHandle;
import com.smallaswater.littlemonster.handle.TimmerKeyHandle;

import java.util.ArrayList;

/**
 * @author SmallasWater
 * Create on 2021/6/2 20:52
 * Package com.smallaswater.customvip.manager
 */
public class KeyHandleManager {

    private static ArrayList<KeyHandle> keyHanle = new ArrayList<>();

    public static boolean isKey(Player player,String key){
        KeyHandle handle = getHandle(player);
        return handle.isKey(key);
    }

    public static void addTimmerKey(Player player,String key,int time){
        KeyHandle handle = getHandle(player);
        handle.addKey(key,true);
        TimeHandle handle1 = TimerHandleManager.getTimeHandle(player);
        TimmerKeyHandle keyHandle = new TimmerKeyHandle(player);
        keyHandle.add(key);
        handle1.registerListener.add(keyHandle);
        handle1.addTimer(key,time);
    }

    public static void addKey(Player player,String key){
       addKey(player, key,true);
    }

    public static Object getKey(Player player,String key){
        KeyHandle handle = getHandle(player);
        return handle.getKey(key);
    }

    public static void addKey(Player player,String key,Object o){
        KeyHandle handle = getHandle(player);
        handle.addKey(key,o);
    }

    public static void removeKey(Player player,String key){
        KeyHandle handle = getHandle(player);
        handle.removeKey(key);
    }

    public static KeyHandle getHandle(Player player){
        KeyHandle handle = new KeyHandle(player);
        if(!keyHanle.contains(handle)){
            keyHanle.add(handle);
        }
        return keyHanle
                .get(keyHanle.indexOf(handle));
    }


}
