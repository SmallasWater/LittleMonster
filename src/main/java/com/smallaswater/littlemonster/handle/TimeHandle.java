package com.smallaswater.littlemonster.handle;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import com.smallaswater.littlemonster.manager.KeyHandleManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;


/**
 * @author SmallasWater
 */
public class TimeHandle {

    private Player player;

    public ArrayList<TimmerKeyHandle> registerListener = new ArrayList<>();

    private LinkedHashMap<String,Integer> times = new LinkedHashMap<>();

    public LinkedHashMap<String, Integer> getTimes() {
        return times;
    }

    public int getCold(String name){
        if(times.containsKey(name)){
            return times.get(name);
        }
        return 0;
    }


    public TimeHandle(Player player){
        this.player = player;
    }

    public void loading(){
        for(String name: times.keySet()){
            times.put(name,times.get(name) - 1);
            player.sendTip(TextFormat.colorize('&',"&b "+name+" 持续中 剩余&r "+times.get(name)+" 秒"));
            if(times.get(name) <= 0){
                TimmerKeyHandle k = new TimmerKeyHandle(player);
                if(registerListener.contains(k)){
                    k = registerListener.get(registerListener.indexOf(k));
                    if(k.hasKey(name)){
                        KeyHandle handle1 = KeyHandleManager.getHandle(player);
                        handle1.onTimerBack(name);
                        k.getKeys().remove(name);
                    }

                }
                times.remove(name);
            }
        }
    }

    public void setCold(String name,int timer) {
        if(times.containsKey(name)){
            times.put(name,timer);
        }

    }

    public void addTimer(String name, int time){
        times.put(name,time);
    }

    public boolean hasCold(String name){
        return times.containsKey(name);
    }


}
