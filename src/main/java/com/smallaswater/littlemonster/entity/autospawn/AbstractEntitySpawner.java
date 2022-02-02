package com.smallaswater.littlemonster.entity.autospawn;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.threads.runnables.AutoSpawnTask;
import com.smallaswater.littlemonster.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author MobPlugin
 */
public abstract class AbstractEntitySpawner implements IEntitySpawner {

    protected String entityName;

    private AutoSpawnTask spawnTask;

    protected Server server;

    private List<String> spawnWorlds = new ArrayList<>();

    public AbstractEntitySpawner(AutoSpawnTask spawnTask) {
        this.spawnTask = spawnTask;
        this.server = Server.getInstance();
        List<String> disabledWorlds = LittleMonsterMainClass.getMasterMainClass().getConfig().getStringList("npcs.worlds-spawning");
        if (disabledWorlds != null && !disabledWorlds.isEmpty()) {
            spawnWorlds = new ArrayList<>(disabledWorlds);
        }

    }

    @Override
    public void spawn(Collection<Player> onlinePlayers) {
        if (this.isSpawnAllowedByDifficulty()) {
            for (Player player : onlinePlayers) {
                if (this.isWorldSpawnAllowed(player.getLevel())) {
                    SpawnResult lastSpawnResult = this.spawn(player);
                    if (lastSpawnResult.equals(SpawnResult.MAX_SPAWN_REACHED)) {
                        break;
                    }
                }
            }
        }

    }

    private boolean isWorldSpawnAllowed(Level level) {
        return spawnWorlds.contains(level.getFolderName()) && level.getGameRules().getBoolean(GameRule.DO_MOB_SPAWNING);

    }

    private SpawnResult spawn(Player player) {
        Position pos = player.getPosition();
        Level level = player.getLevel();
        if (this.spawnTask.entitySpawnAllowed(level, this.getEntityName(), player)) {
            if (pos != null) {
                pos.x += this.spawnTask.getRandomSafeXZCoord(50, 26, 6);
                pos.z += this.spawnTask.getRandomSafeXZCoord(50, 26, 6);
                pos.y = this.spawnTask.getSafeYCoord(level, pos, 3);
                return this.spawn(player, pos, level);
            } else {
                return SpawnResult.POSITION_MISMATCH;
            }
        } else {
            return SpawnResult.MAX_SPAWN_REACHED;
        }
    }

    private boolean isSpawnAllowedByDifficulty() {
        int randomNumber = Utils.rand(0, 3);
        switch(this.server.getDifficulty()) {
            case 0:
                return randomNumber == 0;
            case 1:
                return randomNumber <= 1;
            case 2:
                return randomNumber <= 2;
            default:
                return true;
        }
    }


}