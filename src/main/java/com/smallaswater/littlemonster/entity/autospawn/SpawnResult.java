package com.smallaswater.littlemonster.entity.autospawn;

/**
 * @author MobPlugin
 */
public enum SpawnResult {
    /***/
    MAX_SPAWN_REACHED,
    WRONG_BLOCK,
    WRONG_LIGHTLEVEL,
    POSITION_MISMATCH,
    OK,
    SPAWN_DENIED,
    WRONG_BIOME;

    SpawnResult() {
    }
}
