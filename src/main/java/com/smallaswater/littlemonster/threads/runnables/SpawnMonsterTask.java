package com.smallaswater.littlemonster.threads.runnables;


import cn.nukkit.Server;
import cn.nukkit.level.Position;
import cn.nukkit.utils.TextFormat;
import com.smallaswater.littlemonster.LittleMasterMainClass;
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
        for (PositionConfig easyEntity : LittleMasterMainClass.getMasterMainClass().positions.values()) {
            if(easyEntity.getMoveSize() != -1) {
                for(LittleNpc littleNpc: Utils.getEntitys(easyEntity.getLittleNpc().getName())){
                    if (littleNpc.distance(easyEntity.getPos()) >= easyEntity.getMoveSize()) {
                        littleNpc.teleport(easyEntity.getPos());
                        littleNpc.setHealth(littleNpc.getMaxHealth());
                    }
                }
            }
            if(!easyEntity.isOpen()){
                continue;
            }
            boolean spawn = true;
            if(Utils.getEntityCount(easyEntity.getPos().level,
                    easyEntity.getLittleNpc().getName(),easyEntity.getName())
                    >= easyEntity.getMaxCount()){
                spawn =  false;
            }
            if (spawn) {
                if (LittleMasterMainClass.getMasterMainClass().time.containsKey(easyEntity.getName())) {
                    int t = LittleMasterMainClass.getMasterMainClass().time.get(easyEntity.getName());
                    t--;
                    if (t <= 0) {
                        if(LittleMasterMainClass.getMasterMainClass().monsters.containsKey(easyEntity.getLittleNpc().getName())){
                            if(easyEntity.getConfig().getBoolean("公告.是否提示",true)) {
                                Server.getInstance().broadcastMessage(TextFormat.colorize('&', easyEntity.getConfig()
                                        .getString("公告.复活提醒", "&e[ &bBOSS提醒 &e] &a{name} 已复活")
                                        .replace("{name}", easyEntity.getLittleNpc().getName())));
                            }
                            for(int i = 0;i < easyEntity.getCount();i++) {
                                LittleNpc npc = easyEntity.getLittleNpc().spawn(easyEntity.getPos(),easyEntity.getLiveTime());
                                npc.spawnPos = easyEntity.getName();
                            }
                        }
                        t = easyEntity.getRound();
                    }
                    if(easyEntity.getConfig().getBoolean("公告.是否提示",true)) {
                        for(int i: easyEntity.getConfig().getIntegerList("公告.时间")){
                            if(i == t){
                                Server.getInstance().broadcastMessage(TextFormat.colorize('&', easyEntity.getConfig()
                                        .getString("公告.信息", "&e[ &bBOSS提醒 &e] &a{name} 将在 {time} 后复活")
                                        .replace("{name}", easyEntity.getLittleNpc().getName()).replace("{time}", t + "")));
                            }
                        }
                    }
                    LittleMasterMainClass.getMasterMainClass().time.put(easyEntity.getName(), t);
                } else {
                    LittleMasterMainClass.getMasterMainClass().time.put(easyEntity.getName(),easyEntity.getRound());
                }
            }
        }
        return true;
    }


}
