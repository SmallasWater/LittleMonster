package com.smallaswater.littlemonster.threads.runnables;


import cn.nukkit.Server;
import cn.nukkit.utils.TextFormat;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.config.PositionConfig;
import com.smallaswater.littlemonster.config.SpawnRule;
import com.smallaswater.littlemonster.entity.IEntity;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.utils.Utils;

import static com.smallaswater.littlemonster.utils.Utils.isValidChunk;


/**
 * @author SmallasWater
 * Create on 2021/6/29 8:50
 * Package com.smallaswater.littlemonster.threads.runnables
 */
public class SpawnMonsterTask extends BasePluginThreadTask {

    @Override
    public boolean scheduler() {
        //刷怪
        for (PositionConfig positionConfig : LittleMonsterMainClass.getInstance().positions.values()) {
            if (positionConfig.getMoveSize() != -1) {
                for (LittleNpc littleNpc : Utils.getEntitysByPos(positionConfig)) {
                    if (littleNpc.distance(positionConfig.getPos()) >= positionConfig.getMoveSize()) {
                        littleNpc.teleport(positionConfig.getPos());
                        littleNpc.setHealth(littleNpc.getMaxHealth());
                    }
                }
            }

            if (!positionConfig.isOpen() || !positionConfig.posCanSpawn()) {
                continue;
            }

            boolean spawn = true;
            int ec = Utils.getEntityCount(positionConfig.getPos().level,
                    positionConfig.getLittleNpc().getName(), positionConfig.getName());
            if (ec >= positionConfig.getMaxCount()) {
                spawn = false;
            }

            SpawnRule rule = positionConfig.getSpawnRule();
            boolean scheduled = rule != null && rule.getType() == SpawnRule.Type.SCHEDULED;

            if (spawn) {
                if (scheduled) {
                    tickScheduled(positionConfig, rule);
                } else if (positionConfig.time > 0) {
                    positionConfig.time--;
                    if (positionConfig.time <= 0) {
                        spawnMonster(positionConfig);
                        positionConfig.time = computeNextInterval(positionConfig);
                    }
                    if (positionConfig.getConfig().getBoolean("公告.是否提示", true)) {
                        for (int i : positionConfig.getConfig().getIntegerList("公告.时间")) {
                            if (i == positionConfig.time) {
                                Server.getInstance().broadcastMessage(TextFormat.colorize('&', positionConfig.getConfig()
                                        .getString("公告.信息", "&e[ &bBOSS提醒 &e] &a{name} 将在 {time} 后复活")
                                        .replace("{name}", positionConfig.getLittleNpc().getName()).replace("{time}", Utils.formatSeconds(positionConfig.time))));
                            }
                        }
                    }
                } else {
                    positionConfig.time = computeNextInterval(positionConfig);
                }
            } else if (scheduled) {
                // 存活数达上限时 FIXED 冻结倒数（保留旧行为）；SCHEDULED 仍按墙钟刷新剩余秒数，
                // 冻结期间错过的触发时刻会随重算滚到下一个，解冻后不会补刷。
                positionConfig.time = rule.nextTriggerSeconds(System.currentTimeMillis());
            }
        }
        return true;
    }

    /**
     * SCHEDULED 规则的 tick：每 tick 按墙钟重算剩余秒数，而不是 {@code time--} 倒数。
     * <p>触发判定：本 tick 重算值 &gt; 上一 tick 的值，说明两次 tick 之间越过了一
     * 个触发时刻（该时刻已过，重算跳到再下一个触发点，剩余秒数突变增大）。
     * <p>相比倒数方式的好处：
     * <ul>
     *   <li>线程漂移不累积：{@code time--} 每次迭代会把循环体耗时计入，周期越长
     *       触发时刻偏得越晚；按墙钟重算则每 tick 自动校准。</li>
     *   <li>冻结不错位：存活数达上限时倒数被冻结但墙钟在走，解冻后倒数已失效；
     *       重算方式下冻结期间 time 也持续刷新（见 scheduler 的 else 分支），
     *       错过的时刻不补刷（符合 README 约定）。</li>
     * </ul>
     */
    private static void tickScheduled(PositionConfig positionConfig, SpawnRule rule) {
        int previous = positionConfig.time;
        int remaining = rule.nextTriggerSeconds(System.currentTimeMillis());
        positionConfig.time = remaining;

        if (previous > 0 && remaining > previous) {
            // 剩余秒数突变增大 = 刚越过一个触发时刻
            spawnMonster(positionConfig);
            return;
        }
        if (previous > 0 && positionConfig.getConfig().getBoolean("公告.是否提示", true)) {
            // 重算值受取整/tick 耗时影响可能跳值（如 601 → 599），按「越过阈值」判定而非 ==
            for (int i : positionConfig.getConfig().getIntegerList("公告.时间")) {
                if (previous > i && remaining <= i) {
                    Server.getInstance().broadcastMessage(TextFormat.colorize('&', positionConfig.getConfig()
                            .getString("公告.信息", "&e[ &bBOSS提醒 &e] &a{name} 将在 {time} 后复活")
                            .replace("{name}", positionConfig.getLittleNpc().getName())
                            .replace("{time}", Utils.formatSeconds(remaining))));
                }
            }
        }
    }

    /**
     * 执行刷怪并发送「复活提醒」公告。怪物未注册时仅消耗本次触发，不生成实体。
     */
    private static void spawnMonster(PositionConfig positionConfig) {
        if (!LittleMonsterMainClass.getInstance().monsters.containsKey(positionConfig.getLittleNpc().getName())) {
            return;
        }
        if (positionConfig.getConfig().getBoolean("公告.是否提示", true)) {
            Server.getInstance().broadcastMessage(TextFormat.colorize('&', positionConfig.getConfig()
                    .getString("公告.复活提醒", "&e[ &bBOSS提醒 &e] &a{name} 已复活")
                    .replace("{name}", positionConfig.getLittleNpc().getName())));
        }
        for (int i = 0; i < positionConfig.getCount(); i++) {
            if (isValidChunk(positionConfig.getSpawnPos().getChunk())) {//TODO: 检查为什么会有无效区块
                IEntity npc = positionConfig.getLittleNpc().spawn(positionConfig.getSpawnPos(), positionConfig.getLiveTime());
                npc.setSpawnPos(positionConfig.getName());
            }
        }
    }

    /**
     * 计算 FIXED 规则下一次刷怪的倒数秒数，取代旧版 {@code getRound()}。
     * <p>SCHEDULED 规则不走这里——见 {@link #tickScheduled}，每 tick 按墙钟重算。
     */
    private static int computeNextInterval(PositionConfig pc) {
        SpawnRule rule = pc.getSpawnRule();
        if (rule == null) {
            return 10;
        }
        return rule.nextTriggerSeconds(System.currentTimeMillis());
    }

}
