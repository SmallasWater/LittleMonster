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
                for (PositionConfig positionConfig : LittleMonsterMainClass.getMasterMainClass().positions.values()) {
                    if(!positionConfig.isDispalFloat()){
                        if(manager.hasPosition(positionConfig.getPos())){
                            particle = manager.get(positionConfig.getPos());
                            RemoveEntityPacket pk = new RemoveEntityPacket();
                            pk.eid = particle.getEntityId();
                            player.dataPacket(pk);
                            manager.remove(positionConfig.getPos());
                        }
                        continue;
                    }
                    if (positionConfig.getPos().level.getFolderName().equals(player.getLevel().getFolderName())) {
                        if (!manager.hasPosition(positionConfig.getPos())) {
                            particle = new FlotText(positionConfig.getName(), positionConfig.getPos().add(0.5, 2, 0.5), positionConfig.getTitle(), player);
                            positionConfig.getPos().getLevel().addParticle(particle, player);
                            manager.add(particle);
                        } else {

                            particle = manager.get(positionConfig.getPos());
                            particle.setTitle(positionConfig.getTitle());
                            String text = positionConfig.getText();
                            int time = positionConfig.time;
                            int entityCount = Utils.getEntityCount(positionConfig.getPos().level, positionConfig.getLittleNpc().getName(), positionConfig.getName());
                            text = text.replace("{名称}", positionConfig.getLittleNpc().getName())
                                    .replace("{数量}", entityCount + "")
                                    .replace("{上限}", positionConfig.getMaxCount() + "")
                                    .replace("{time}", (entityCount >= positionConfig.getMaxCount() ? "已刷新" : String.valueOf(time)))
                                    .replace("{name}", player.getName());
                            particle.setText(text);
                            particle.toUpData();
                        }
                    } else {
                        if (manager.hasPosition(positionConfig.getPos())) {
                            particle = manager.get(positionConfig.getPos());
                            RemoveEntityPacket pk = new RemoveEntityPacket();
                            pk.eid = particle.getEntityId();
                            player.dataPacket(pk);
                            manager.remove(positionConfig.getPos());
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
