package com.smallaswater.littlemonster.events.entity;

import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.entity.EntityDamageEvent;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.entity.IEntity;

import java.util.HashMap;

/**
 * Handles the event when a Little Monster entity dies and drops experience.
 * Allows the event to be cancelled if needed.
 *
 * @author LT_Name
 */
public class LittleMonsterEntityDeathDropExpEvent extends LittleMonsterEntityEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    public static String DROP_EXP_ORIGIN = "origin";

    /**
     * Constructs a new LittleMonsterEntityDeathDropExpEvent.
     *
     * @param entity            The entity that died.
     * @param dropExp           The amount of experience to drop.
     * @param eventDamageEvent  The damage event that caused the death.
     */
    public LittleMonsterEntityDeathDropExpEvent(IEntity entity, int dropExp, EntityDamageEvent eventDamageEvent) {
        super(entity);
        this.setStoredExp(DROP_EXP_ORIGIN, dropExp);
        this.eventDamageEvent = eventDamageEvent;
    }

    private final HashMap<String, Integer> dropExpMap = new HashMap<>();

    private int totalExp = 0;

    /**
     * Gets the sum of all experience values in the HashMap.
     *
     * @return Total experience.
     */
    public int getTotalExp() {
        return totalExp;
    }

    /**
     * Calculates the difference between origin and total experience.
     *
     * @return The difference between origin experience and total experience.
     */
    public int getDifference() {
        return getOriginExp() - getTotalExp();
    }

    /**
     * Retrieves the origin experience value.
     *
     * @return The origin experience value.
     */
    public int getOriginExp() {
        return getStoredExp(DROP_EXP_ORIGIN);
    }

    /**
     * Retrieves the stored experience value for the specified key.
     *
     * @param key The key name.
     * @return The corresponding experience value, or 0 if the key does not exist.
     */
    public int getStoredExp(String key) {
        return dropExpMap.getOrDefault(key, 0);
    }

    /**
     * Sets the stored experience value for the specified key.
     *
     * @param key   The key name.
     * @param value The experience value to set.
     */
    public void setStoredExp(String key, int value) {
        dropExpMap.put(key, value);
        totalExp += value;
    }

    /**
     * Prints all experience values in the HashMap.
     */
    public void printExpMap() {
        LittleMonsterMainClass.getInstance().getLogger().info("Total experience dropped (" + totalExp + "):");
        for (HashMap.Entry<String, Integer> entry : dropExpMap.entrySet()) {
            LittleMonsterMainClass.getInstance().getLogger().info("Key: " + entry.getKey() + " - Experience: " + entry.getValue());
        }
    }

    protected EntityDamageEvent eventDamageEvent;

    /**
     * Gets the damage event that caused the entity's death.
     *
     * @return The EntityDamageEvent.
     */
    public EntityDamageEvent getEventDamageEvent() {
        return this.eventDamageEvent;
    }
}
