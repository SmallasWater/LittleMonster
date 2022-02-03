package com.smallaswater.littlemonster.config;

import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Config;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import lombok.Data;

import java.util.Map;

/**
 * @author SmallasWater
 * Create on 2021/7/1 17:13
 * Package com.smallaswater.littlemonster.config
 */
@Data
public class PositionConfig {

    private Position pos;

    private boolean open;

    private String name;

    private MonsterConfig littleNpc;

    private Config config;

    private int moveSize = 10;

    private int liveTime = -1;

    private double size = 1;

    private int count;

    private int round;

    private int maxCount;

    private String title;

    private String text;

    private int spawnSize;

    private boolean dispalFloat;

    public int time = -1;


    private PositionConfig(String name, Config config){
        this.name = name;
        this.config = config;
    }

    public boolean isDispalFloat() {
        return dispalFloat;
    }

    public static PositionConfig loadPosition(String name, Config config) {
        PositionConfig entity = new PositionConfig(name,config);
        MonsterConfig monsterConfig = LittleMonsterMainClass.getMasterMainClass().monsters
                .getOrDefault(config.getString("刷新怪物",null),null);
        if(monsterConfig == null){
            return null;
        }
        entity.setLittleNpc(monsterConfig);
        entity.setOpen(config.getBoolean("是否刷怪",true));
        entity.setLiveTime(config.getInt("怪物存在时间",30));
        entity.setDispalFloat(config.getBoolean("是否显示刷怪点",true));
        entity.setMoveSize(config.getInt("怪物最大位移距离",10));
        entity.setSpawnSize(config.getInt("刷怪距离",18));
        entity.setText(config.getString("刷怪点浮空字",
                "§e>>§a----------------------§e<<\n" +
                        "  §e>>§a{名称} 的刷怪点       §e<<\n" +
                        "  §e>>§a数量: {数量} / {上限} §e<<\n" +
                        "  §e>>§a刷新时间: {time}      §e<<\n" +
                        "  §e>>§a----------------------§e<<"));
        entity.setTitle(config.getString("刷怪点标题","§a>>§l§o LittleMonster §r§a<< "));
        entity.setMaxCount(config.getInt("刷怪上限"));
        entity.setRound(config.getInt("刷怪间隔"));
        entity.setCount(config.getInt("刷怪数量"));
        Map pos = (Map) config.get("刷怪点");
        String levelName = ((String) pos.get("level")).trim();
        Level l = null;
        if(!Server.getInstance().isLevelLoaded(levelName)){
            if(Server.getInstance().loadLevel(levelName)){
                l = Server.getInstance().getLevelByName(levelName);
            }else{
                Server.getInstance().getLogger().warning("加载地图 "+levelName+"失败");
            }

        }else{
            l = Server.getInstance().getLevelByName(levelName);
        }
        if(l == null){
            return null;
        }
        Position position =  new Position((double) pos.get("x"),(double) pos.get("y"),(double) pos.get("z"),l);
        entity.setPos(position);
        return entity;
    }
}
