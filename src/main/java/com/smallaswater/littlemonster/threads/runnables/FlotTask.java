package com.smallaswater.littlemonster.threads.runnables;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.config.PositionConfig;
import com.smallaswater.littlemonster.flot.FlotText;
import com.smallaswater.littlemonster.manager.PlayerFlotTextManager;
import com.smallaswater.littlemonster.utils.Utils;

import java.util.ArrayList;



/**
 * @author SmallasWater
 * Create on 2021/6/29 8:21
 * Package com.smallaswater.littlemonster.threads.runnables
 */
public class FlotTask extends BasePluginThreadTask{

    @Override
    public boolean scheduler() {
        FlotText particle;
        for(Player player: Server.getInstance().getOnlinePlayers().values()) {
            PlayerFlotTextManager manager = PlayerFlotTextManager.getInstance(player);
            if (player.isOnline()) {
                for (PositionConfig easyEntity : LittleMonsterMainClass.getMasterMainClass().positions.values()) {
                    if(!easyEntity.isDispalFloat()){
                        if(manager.hasPosition(easyEntity.getPos())){
                            particle = manager.get(easyEntity.getPos());
                            RemoveEntityPacket pk = new RemoveEntityPacket();
                            pk.eid = particle.getEntityId();
                            player.dataPacket(pk);
                            manager.remove(easyEntity.getPos());
                        }
                        continue;
                    }
                    if (easyEntity.getPos().level.getFolderName().equals(player.getLevel().getFolderName())) {
                        if (!manager.hasPosition(easyEntity.getPos())) {
                            particle = new FlotText(easyEntity.getName(), easyEntity.getPos().add(0.5, 2, 0.5), easyEntity.getTitle(), player);
                            easyEntity.getPos().getLevel().addParticle(particle, player);
                            manager.add(particle);
                        } else {

                            particle = manager.get(easyEntity.getPos());
                            particle.setTitle(easyEntity.getTitle());
                            String text = easyEntity.getText();
                            text = text.replace("{名称}", easyEntity.getLittleNpc().getName())
                                    .replace("{数量}", Utils
                                            .getEntityCount(easyEntity.getPos().level, easyEntity.getLittleNpc().getName(),easyEntity.getName()) + "")
                                    .replace("{上限}", easyEntity.getMaxCount() + "")
                                    .replace("{time}", LittleMonsterMainClass.getMasterMainClass().time.containsKey(easyEntity.getName()) ?
                                            LittleMonsterMainClass.getMasterMainClass().time.get(easyEntity.getName()) + "" : "0")
                                    .replace("{name}", player.getName());
                            particle.setText(text);
                            particle.toUpData();
                        }
                    } else {
                        if (manager.hasPosition(easyEntity.getPos())) {
                            particle = manager.get(easyEntity.getPos());
                            RemoveEntityPacket pk = new RemoveEntityPacket();
                            pk.eid = particle.getEntityId();
                            player.dataPacket(pk);
                            manager.remove(easyEntity.getPos());
                        }
                    }
                }
            } else {
                if (manager.size() > 0) {
                    for (FlotText position : manager.getFlotTexts()) {
                        RemoveEntityPacket pk = new RemoveEntityPacket();
                        pk.eid = position.getEntityId();
                        player.dataPacket(pk);
                        manager.remove(position);
                    }
                }
                LittleMonsterMainClass.getMasterMainClass().texts.clear();
            }
            if (PlayerFlotTextManager.getInstance(player).getFlotTexts().size() > 0) {
                try {
                    ArrayList< FlotText> texts = PlayerFlotTextManager.getInstance(player).getFlotTexts();
                    if (texts.size() > 0) {
                        for (FlotText position : texts) {
                            if (!LittleMonsterMainClass.getMasterMainClass().positions.containsKey(position.getName())) {
                                RemoveEntityPacket pk = new RemoveEntityPacket();
                                pk.eid = position.getEntityId();
                                player.dataPacket(pk);
                                texts.remove(position);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return true;
    }
}
