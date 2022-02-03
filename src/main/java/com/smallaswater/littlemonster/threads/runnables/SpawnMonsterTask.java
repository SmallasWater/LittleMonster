package com.smallaswater.littlemonster.threads.runnables;


import cn.nukkit.Server;
import cn.nukkit.utils.TextFormat;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.config.PositionConfig;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.utils.Utils;


/**
 * @author SmallasWater
 * Create on 2021/6/29 8:50
 * Package com.smallaswater.littlemonster.threads.runnables
 */
public class SpawnMonsterTask extends BasePluginThreadTask {

    @Override
    public boolean scheduler() {
        //刷怪
        for (PositionConfig positionConfig : LittleMonsterMainClass.getMasterMainClass().positions.values()) {
            if(positionConfig.getMoveSize() != -1) {
                for(LittleNpc littleNpc: Utils.getEntitysByPos(positionConfig)){
                    if (littleNpc.distance(positionConfig.getPos()) >= positionConfig.getMoveSize()) {
                        littleNpc.teleport(positionConfig.getPos());
                        littleNpc.setHealth(littleNpc.getMaxHealth());
                    }
                }
            }
            if(!positionConfig.isOpen()){
                continue;
            }
            boolean spawn = true;
            int ec = Utils.getEntityCount(positionConfig.getPos().level,
                    positionConfig.getLittleNpc().getName(),positionConfig.getName());
            if(ec >= positionConfig.getMaxCount()){

                spawn =  false;
            }

            if (spawn) {
                if (LittleMonsterMainClass.getMasterMainClass().time.containsKey(positionConfig.getName())) {
                    int t = LittleMonsterMainClass.getMasterMainClass().time.get(positionConfig.getName());
                    t--;
                    if (t <= 0) {
                        if(LittleMonsterMainClass.getMasterMainClass().monsters.containsKey(positionConfig.getLittleNpc().getName())){
                            if(positionConfig.getConfig().getBoolean("公告.是否提示",true)) {
                                Server.getInstance().broadcastMessage(TextFormat.colorize('&', positionConfig.getConfig()
                                        .getString("公告.复活提醒", "&e[ &bBOSS提醒 &e] &a{name} 已复活")
                                        .replace("{name}", positionConfig.getLittleNpc().getName())));
                            }
                            for(int i = 0;i < positionConfig.getCount();i++) {
                                LittleNpc npc = positionConfig.getLittleNpc().spawn(positionConfig.getPos(),positionConfig.getLiveTime());
                                npc.spawnPos = positionConfig.getName();
                            }
                        }
                        t = positionConfig.getRound();
                    }
                    if(positionConfig.getConfig().getBoolean("公告.是否提示",true)) {
                        for(int i: positionConfig.getConfig().getIntegerList("公告.时间")){
                            if(i == t){
                                Server.getInstance().broadcastMessage(TextFormat.colorize('&', positionConfig.getConfig()
                                        .getString("公告.信息", "&e[ &bBOSS提醒 &e] &a{name} 将在 {time} 后复活")
                                        .replace("{name}", positionConfig.getLittleNpc().getName()).replace("{time}", t + "")));
                            }
                        }
                    }
                    LittleMonsterMainClass.getMasterMainClass().time.put(positionConfig.getName(), t);
                } else {
                    LittleMonsterMainClass.getMasterMainClass().time.put(positionConfig.getName(),positionConfig.getRound());
                }
            }
        }
        return true;
    }


}
