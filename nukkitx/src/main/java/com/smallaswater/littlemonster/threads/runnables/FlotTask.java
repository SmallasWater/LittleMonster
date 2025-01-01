package com.smallaswater.littlemonster.threads.runnables;

import cn.nukkit.Player;
import cn.nukkit.Server;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.config.PositionConfig;
import com.smallaswater.littlemonster.flot.FlotText;
import com.smallaswater.littlemonster.manager.PlayerFlotTextManager;
import com.smallaswater.littlemonster.utils.Utils;

import java.util.Iterator;


/**
 * 浮空字任务
 *
 * @author SmallasWater
 * Create on 2021/6/29 8:21
 * Package com.smallaswater.littlemonster.threads.runnables
 */
public class FlotTask extends BasePluginThreadTask {

    @Override
    public boolean scheduler() {
        FlotText particle;
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            if (player.isOnline()) {
                PlayerFlotTextManager manager = PlayerFlotTextManager.getOrCreate(player);
                for (PositionConfig positionConfig : LittleMonsterMainClass.getInstance().positions.values()) {
                    if (!positionConfig.isDispalFloat()) {
                        if (manager.hasPosition(positionConfig.getPos())) {
                            particle = manager.get(positionConfig.getPos());
                            particle.close();
                            manager.remove(positionConfig.getPos());
                        }
                        continue;
                    }
                    if (positionConfig.getPos().level == player.getLevel()) {
                        if (!manager.hasPosition(positionConfig.getPos())) {
                            particle = new FlotText(positionConfig.getName(), positionConfig.getPos().add(0.5, 2, 0.5), positionConfig.getTitle(), player);
                            positionConfig.getPos().getLevel().addParticle(particle, player);
                            manager.add(particle);
                        }
                        particle = manager.get(positionConfig.getPos());
                        if (particle != null) {
                            particle.setTitle(positionConfig.getTitle());
                            int entityCount = Utils.getEntityCount(positionConfig.getPos().level, positionConfig.getLittleNpc().getName(), positionConfig.getName());
                            String text = positionConfig.getText()
                                    .replace("{名称}", positionConfig.getLittleNpc().getName())
                                    .replace("{数量}", entityCount + "")
                                    .replace("{上限}", positionConfig.getMaxCount() + "")
                                    .replace("{time}", (entityCount >= positionConfig.getMaxCount() ? "已刷新" : String.valueOf(positionConfig.time)))
                                    .replace("{name}", player.getName());
                            particle.setText(text);
                            particle.toUpData();
                        }
                    } else {
                        if (manager.hasPosition(positionConfig.getPos())) {
                            particle = manager.get(positionConfig.getPos());
                            particle.close();
                            manager.remove(positionConfig.getPos());
                        }
                    }
                }
            }
            /*if (PlayerFlotTextManager.getOrCreate(player).getFlotTexts().size() > 0) {
                try {
                    ArrayList<FlotText> texts = PlayerFlotTextManager.getOrCreate(player).getFlotTexts();
                    if (texts.size() > 0) {
                        for (FlotText position : texts) {
                            if (!LittleMonsterMainClass.getMasterMainClass().positions.containsKey(position.getName())) {
                                position.close();
                                texts.remove(position);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }*/
        }

        //清理不在线玩家的浮空字
        Iterator<PlayerFlotTextManager> it = LittleMonsterMainClass.getInstance().playerFlotTextManagers.iterator();
        while (it.hasNext()) {
            PlayerFlotTextManager manager = it.next();
            if (!manager.getPlayer().isOnline()) {
                if (manager.size() > 0) {
                    Iterator<FlotText> iterator = manager.getFlotTexts().iterator();
                    while (iterator.hasNext()) {
                        FlotText flotText = iterator.next();
                        flotText.close();
                        iterator.remove();
                    }
                }
                it.remove();
            }
        }

        return true;
    }
}
