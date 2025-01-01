package com.smallaswater.littlemonster.handle;

import cn.nukkit.Player;

import java.util.ArrayList;

/**
 * @author SmallasWater
 * Create on 2021/6/29 20:56
 * Package com.smallaswater.littlemonster.handle
 */
public class TimmerKeyHandle {

    private Player player;

    private ArrayList<String> keys = new ArrayList<>();

    public TimmerKeyHandle(Player player){
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void add(String key){
        keys.add(key);
    }

    public void setKeys(ArrayList<String> keys) {
        this.keys = keys;
    }

    public ArrayList<String> getKeys() {
        return keys;
    }

    public boolean hasKey(String key){
        return keys.contains(key);
    }
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof TimmerKeyHandle){
            return ((TimmerKeyHandle) obj).player.equals(player);
        }
        return false;
    }
}
