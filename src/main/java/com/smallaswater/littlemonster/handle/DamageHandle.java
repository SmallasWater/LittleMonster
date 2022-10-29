package com.smallaswater.littlemonster.handle;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.TextFormat;
import com.smallaswater.littlemonster.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author SmallasWater
 * Create on 2021/6/28 7:56
 * Package com.smallaswater.littlemonster.handle
 */
public class DamageHandle {

    public LinkedHashMap<String, Double> playerDamageList = new LinkedHashMap<>();

    public void add(Player player, double damage) {
        this.add(player.getName(), damage);
    }

    public void add(String playerName, double damage) {
        if (!playerDamageList.containsKey(playerName)) {
            playerDamageList.put(playerName, damage);
        } else {
            double d1 = playerDamageList.get(playerName) + damage;
            playerDamageList.put(playerName, d1);
        }
    }

    private final ArrayList<String> keyPlayers = new ArrayList<>();

    public void display() {
        keyPlayers.clear();
        FormWindowSimple simple = new FormWindowSimple(TextFormat.colorize('&', "&l&a击杀伤害排行榜"), TextFormat.colorize('&', "&f___________________"));
        StringBuilder builder = new StringBuilder();
        LinkedHashMap<String, Number> lists = Utils.toRankList(playerDamageList);
        int i = 1;
        for (Map.Entry<String, Number> name : lists.entrySet()) {
            builder.append(TextFormat.colorize('&', "&aNo." + i + " &a玩家: &f" + name.getKey() + "   &e伤害: &f" + name.getValue())).append("\n");
            i++;
        }
        simple.setContent(builder.toString());
        for (String name : playerDamageList.keySet()) {
            Player player = Server.getInstance().getPlayer(name);
            if (!keyPlayers.contains(name)) {
                keyPlayers.add(name);
                if (player != null) {
                    player.showFormWindow(simple, 103);
                }
            }
        }
    }

    public double get(Player player) {
        return this.get(player.getName());
    }

    public double get(String playerName) {
        if (!playerDamageList.containsKey(playerName)) {
            return 0;
        }
        return playerDamageList.get(playerName);
    }
}
