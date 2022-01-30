package com.smallaswater.littlemonster.handle;

import cn.nukkit.Player;

import java.util.LinkedHashMap;

/**
 * @author SmallasWater
 * Create on 2021/6/2 20:46
 * Package com.smallaswater.customvip.utils
 */
public class KeyHandle {

    private Player player;

    private LinkedHashMap<String,Object> keys = new LinkedHashMap<>();

    public KeyHandle(Player player){
        this.player = player;
    }

    public boolean isKey(String key){
        return keys.containsKey(key);
    }

    public void removeKey(String key){
        keys.remove(key);
    }

    public void addKey(String key){
        if(!keys.containsKey(key)){
            keys.put(key,true);

        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public Object getKey(String key){
        if(keys.containsKey(key)){
            return keys.get(key);
        }
        return null;
    }

    public void addKey(String key,Object o){
        keys.put(key,o);

    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof KeyHandle){
            return ((KeyHandle) obj).player.equals(player);
        }
        return false;
    }

    public void onTimerBack(String name){
        removeKey(name);
    }
}
