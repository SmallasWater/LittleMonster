package com.smallaswater.littlemonster.entity.autospawn;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;

import java.util.Collection;

/**
 * @author MobPlugin
 */
public interface IEntitySpawner {

    String getEntityName();

    void spawn(Collection<Player> var1);

    SpawnResult spawn(Player var1, Position var2, Level var3);

}
