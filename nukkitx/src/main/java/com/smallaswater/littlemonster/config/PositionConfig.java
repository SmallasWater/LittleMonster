package com.smallaswater.littlemonster.config;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Config;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.route.BlockCache;
import com.smallaswater.littlemonster.utils.Utils;
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

    private int posOffset = -1;

    private boolean open;

    private String name;

    private MonsterConfig littleNpc;

    private Config config;

    private int moveSize = 10;

    private int liveTime = -1;

    private double size = 1;

    private int count;

    /**
     * 刷新规则（固定间隔或定时刷新）。
     * <p>取代旧版标量 {@code round}（刷怪间隔秒数）。旧配置会在
     * {@link SpawnRule#load(Config)} 中自动升级为 FIXED 规则。
     */
    private SpawnRule spawnRule;

    private int maxCount;

    private String title;

    private String text;

    private int spawnSize;

    private boolean dispalFloat;

    public int time = -1;

    /** 配置文件中保存剩余刷新秒数的 key（单位：秒） */
    private static final String REMAINING_TIME_KEY = "刷怪剩余秒数";


    private PositionConfig(String name, Config config){
        this.name = name;
        this.config = config;
    }

    /**
     * 将当前剩余刷新秒数持久化到配置文件。
     * 在服务器关闭（onDisable）时调用，保证重启后从剩余秒数继续倒计时。
     * <p>仅在 time &gt; 0 时写入；time &le; 0（未开始倒计时）则移除历史 key，
     * 避免把无意义的 -1 写入用户可见的配置文件。
     * <p><b>仅 FIXED 规则持久化</b>：SCHEDULED 规则的 time 是相对墙钟的绝对时刻换算值，
     * 停机期间不计时，重启后必须基于当前时间重算（见 {@link #loadPosition}），
     * 否则会把"定时刷新"错位成"重启后 N 秒刷新"。
     */
    public void saveTime() {
        if (this.config == null) {
            return;
        }
        if (this.spawnRule != null && this.spawnRule.getType() == SpawnRule.Type.FIXED && this.time > 0) {
            this.config.set(REMAINING_TIME_KEY, this.time);
        } else {
            this.config.remove(REMAINING_TIME_KEY);
        }
        this.config.save();
    }

    public boolean isDispalFloat() {
        return dispalFloat;
    }

    public boolean posCanSpawn() {
        return this.pos.isValid() && /*this.pos.getChunk().isLoaded() && */!this.pos.getLevel().getPlayers().isEmpty();
    }

    public Position getSpawnPos() {
        if (this.posOffset <= 0) {
            return this.pos;
        }
        Position position = this.pos.add(Utils.rand(-this.posOffset, this.posOffset), 0, Utils.rand(-this.posOffset, this.posOffset));
        for (int y=-3; y<=3; y++) {
            Block block = BlockCache.get(position.getLevel()).getBlock((int) position.x, (int) position.y + y, (int) position.z);
            Block up;
            if (!block.canPassThrough() && (up = block.up()).canPassThrough() && up.up().canPassThrough()) {
                position.setY(position.getY() + y + 1);
                break;
            }
        }
        return position;
    }

    public static PositionConfig loadPosition(String name, Config config) {
        PositionConfig entity = new PositionConfig(name, config);
        MonsterConfig monsterConfig = LittleMonsterMainClass.getInstance().monsters
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
        // 解析刷新规则：旧版标量「刷怪间隔」会被自动升级为新版「刷新规则」map 并写回文件
        entity.setSpawnRule(SpawnRule.load(config));
        entity.setCount(config.getInt("刷怪数量"));
        Map pos = (Map) config.get("刷怪点");
        String levelName = ((String) pos.get("level")).trim();
        if(!Server.getInstance().loadLevel(levelName)) {
            Server.getInstance().getLogger().warning("加载地图 "+levelName+"失败");
            return null;
        }
        Level level = Server.getInstance().getLevelByName(levelName);
        if(level == null){
            return null;
        }
        Position position = new Position((double) pos.get("x"),(double) pos.get("y"),(double) pos.get("z"),level);
        entity.setPos(position);
        entity.setPosOffset(config.getInt("刷怪随机偏移距离"));
        // 读取上次关闭时保存的剩余秒数。
        // 仅 FIXED 规则使用持久化秒数：它的 time 就是「固定间隔剩余」语义，停机不计时符合预期。
        // SCHEDULED 规则的 time 是相对墙钟算出的「到下一个绝对触发时刻的剩余」，停机后已失效。
        boolean removedStale = false;
        int savedTime = config.getInt(REMAINING_TIME_KEY, -1);
        if (savedTime > 0 && entity.spawnRule != null && entity.spawnRule.getType() == SpawnRule.Type.FIXED) {
            entity.time = savedTime;
        } else if (savedTime > 0) {
            // 历史残留（规则曾为 FIXED 后切换为 SCHEDULED 等）：清理避免误导
            config.remove(REMAINING_TIME_KEY);
            removedStale = true;
        }
        // SCHEDULED 规则不依赖持久化秒数（停机后已失效），主动按当前墙钟算出初值，
        // 避免 SpawnMonsterTask 首次 tick 前仍为 -1 导致 FlotTask 短暂显示 "0秒"。
        // tickScheduled 的触发判定 (previous>0 && remaining>previous) 不受影响：
        // 首次 tick 的 remaining 仅会更小（未越界）或明显变大（越界=到点该刷）。
        if (entity.spawnRule != null && entity.spawnRule.getType() == SpawnRule.Type.SCHEDULED) {
            entity.time = entity.spawnRule.nextTriggerSeconds(System.currentTimeMillis());
        }
        // 残留 key 的 remove 发生在 SpawnRule.load() 之后，writeBack 的 save 无法覆盖它；
        // 单独 save 确保清理持久化（SCHEDULED 场景尤其重要，否则残留会永久留存）。
        if (removedStale) {
            config.save();
        }
        return entity;
    }
}
