package com.smallaswater.littlemonster.events;


import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.form.element.Element;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.item.Item;
import com.smallaswater.littlemonster.LittleMasterMainClass;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.items.DeathCommand;
import com.smallaswater.littlemonster.items.DropItem;
import com.smallaswater.littlemonster.manager.KeyHandleManager;
import com.smallaswater.littlemonster.manager.TimerHandleManager;
import com.smallaswater.littlemonster.utils.Utils;
import com.smallaswater.littlemonster.windows.LittleWindow;

import java.util.LinkedList;


/**
 * @author SmallasWater
 * Create on 2021/6/28 8:58
 * Package com.smallaswater.littlemonster.events
 */
public class LittleMasterListener implements Listener {

    @EventHandler
    public void onDamage(EntityDamageEvent e){
        if(e instanceof EntityDamageByEntityEvent){
            Entity entity = e.getEntity();
            Entity d = ((EntityDamageByEntityEvent) e).getDamager();
            if(entity instanceof LittleNpc){
                //卡墙修复
                if(e.getCause() == EntityDamageEvent.DamageCause.FALL){
                    entity.teleport(entity.add(0,2));
                }
                if(d instanceof Player){
                    ((LittleNpc) entity).handle.add(d.getName(),e.getDamage());
                }
                float damage = e.getDamage() - ((LittleNpc) entity).getConfig().getDelDamage();
                if(damage < 0){
                    damage = 0;
                }
                e.setDamage(damage);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event){
        Player player = event.getPlayer();
        if(KeyHandleManager.isKey(player,"Ice") && TimerHandleManager.getTimeHandle(player).hasCold("Ice")){
            player.sendPopup("你被冰冻啦");
            event.setCancelled();
        }else{
            KeyHandleManager.removeKey(player,"Ice");
        }
    }

    @EventHandler
    public void onDie(EntityDeathEvent e){
        Entity entity = e.getEntity();

        if(entity instanceof LittleNpc){
            ((LittleNpc) entity).onDeath(e);

        }
    }



    @EventHandler
    public void onListenerWindow(PlayerFormRespondedEvent event) {
        if (event.wasClosed()) {
            return;
        }
        Player player = event.getPlayer();
        if (LittleWindow.WINDOWS.containsKey(event.getFormID())) {
            if(event.getWindow() instanceof FormWindowSimple) {
                String master = ((FormResponseSimple) event.getResponse()).getClickedButton().getText();
                KeyHandleManager.addKey(player, "menu", master);
                LittleWindow.sendSetting(player);
            }else{
                FormWindow window = event.getWindow();
                if(window instanceof FormWindowCustom){
                    MonsterConfig config = LittleMasterMainClass.getMasterMainClass().monsters.get(KeyHandleManager.getKey(player,"menu").toString());
                    String h = ((FormResponseCustom)event.getResponse()).getInputResponse(0);
                    if(h != null){
                        config.setHealth(Integer.parseInt(h));
                        config.set("血量",Integer.parseInt(h));
                    }
                    String d = ((FormResponseCustom)event.getResponse()).getInputResponse(1);
                    if(d != null){
                        config.setDamage(Integer.parseInt(d));
                        config.set("攻击力",Integer.parseInt(d));
                    }
                    String s = ((FormResponseCustom)event.getResponse()).getInputResponse(2);
                    if(s != null){
                        config.setSize(Double.parseDouble(s));
                        config.set("大小",Double.parseDouble(s));
                    }
                    String p = ((FormResponseCustom)event.getResponse()).getInputResponse(3);
                    if(p != null){
                        config.setMoveSpeed(Double.parseDouble(p));
                        config.set("移动速度",Double.parseDouble(p));
                    }
                    String k = ((FormResponseCustom)event.getResponse()).getDropdownResponse(4).getElementContent();
                    if(k != null){
                        config.setSkin(k);
                        config.set("皮肤",k);
                    }
                    config.saveAll();
                    config.resetEntity();
                    player.sendMessage("保存完成");


                }

            }
        }
    }




}
