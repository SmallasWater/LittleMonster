package com.smallaswater.littlemonster.manager;

import cn.nukkit.Player;
import cn.nukkit.utils.BlockColor;
import cn.nukkit.utils.BossBarColor;
import cn.nukkit.utils.DummyBossBar;

import java.util.LinkedHashMap;

/**
 * @author SmallasWater
 * Create on 2021/6/28 7:53
 * Package com.smallaswater.littlemonster.manager
 */
public class BossBarManager {

    public static LinkedHashMap<Player,BossBarApi> apis = new LinkedHashMap<>();

     public static class BossBarApi extends DummyBossBar.Builder {

         private final long bossBarId;

         private BossBarApi(Player player,long id) {
             super(player);
             this.bossBarId = id;
         }


         public static void createBossBar(Player player,long bossBarId) {
             if (!apis.containsKey(player)) {
                 BossBarApi bossBar = new BossBarApi(player,bossBarId);
                 bossBar.length(0);
                 bossBar.text("加载中");
                 apis.put(player, bossBar);
                 player.createBossBar(apis.get(player).build());
             }

         }


         public static void removeBossBar(Player player){
             if(apis.containsKey(player)){
                 if(player.getDummyBossBar(apis.get(player).build().getBossBarId()) != null) {
                     player.removeBossBar(apis.get(player).build().getBossBarId());
                 }
                 apis.remove(player);
             }
         }

         public static boolean hasCreate(Player player,long bossBarId){
             return player.getDummyBossBar(bossBarId) != null;
         }

         public static void showBoss(Player player, String text, float health,float maxHealth) {
             if (apis.get(player) != null) {
                 DummyBossBar bossBar = apis.get(player).build();
                 bossBar.setText(text);

                 bossBar.setLength((float) Math.round(health / maxHealth * 100.0F));
                 try {
                     Class.forName("cn.nukkit.utils.BossBarColor");
                     bossBar.setColor(BossBarColor.WHITE);
                 } catch (ClassNotFoundException ignore) {}

                 player.createBossBar(bossBar);
             }
         }
     }

}
