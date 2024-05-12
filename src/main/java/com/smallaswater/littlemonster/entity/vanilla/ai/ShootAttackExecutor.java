package com.smallaswater.littlemonster.entity.vanilla.ai;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.entity.data.LongEntityData;
import cn.nukkit.entity.projectile.EntityArrow;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.entity.EntityShootBowEvent;
import cn.nukkit.event.entity.ProjectileLaunchEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBow;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Sound;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.entity.IEntity;
import com.smallaswater.littlemonster.entity.vanilla.VanillaNPC;

public class ShootAttackExecutor {
    //protected float speed;
    //protected int maxShootDistanceSquared;
    //protected int coolDownTick;
    //protected boolean inExecute;

    //protected float damage;
    public boolean execute(IEntity ientity, Entity target) {
        //inExecute = true;
        if (ientity.isVanillaEntity()) {
            VanillaNPC vanillaNPC = (VanillaNPC) ientity.getEntity();
            //this.speed = (float) vanillaNPC.speed;
            //this.maxShootDistanceSquared = (int) (vanillaNPC.getConfig().getAttackDistance() * vanillaNPC.getConfig().getAttackDistance());
            //this.coolDownTick = vanillaNPC.getAttackSleepTick();
            //this.damage = vanillaNPC.getDamage();
            playBowAnimation(ientity, target);
            LittleMonsterMainClass.getInstance().getServer().getScheduler().scheduleDelayedTask(()->{
                //inExecute = false;
                bowShoot(ientity, target);
                stopBowAnimation(ientity);
            }, 40);
        } else {
            throw new RuntimeException("Not support Entity type.");
        }

        return true;
    }

    protected static void bowShoot(IEntity ientity, Entity target) {
        ItemBow bow = (ItemBow) Item.get(261);
        EntityLiving entity = (EntityLiving) ientity.getEntity();

        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<DoubleTag>("Pos")
                        .add(new DoubleTag("", entity.x))
                        .add(new DoubleTag("", entity.y + entity.getHeight() / 2 + 0.2f))
                        .add(new DoubleTag("", entity.z)))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", -Math.sin(entity.yaw / 180 * Math.PI) * Math.cos(entity.pitch / 180 * Math.PI)))
                        .add(new DoubleTag("", -Math.sin(entity.pitch / 180 * Math.PI)))
                        .add(new DoubleTag("", Math.cos(entity.yaw / 180 * Math.PI) * Math.cos(entity.pitch / 180 * Math.PI))))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", (entity.yaw > 180 ? 360 : 0) - (float) entity.yaw))
                        .add(new FloatTag("", (float) -entity.pitch)))
                .putDouble("damage", ientity.getConfig().getDamage());
        double f = 3;

        EntityArrow arrow = (EntityArrow) Entity.createEntity("Arrow", entity.chunk, nbt, entity, false);

        if (arrow == null) {
            return;
        }

        EntityShootBowEvent entityShootBowEvent = new EntityShootBowEvent(entity, bow, arrow, f);
        Server.getInstance().getPluginManager().callEvent(entityShootBowEvent);
        EntityProjectile projectile = entityShootBowEvent.getProjectile();
        if (entityShootBowEvent.isCancelled()) {
            projectile.kill();
        } else {
            projectile.setMotion(entityShootBowEvent.getProjectile().getMotion().multiply(entityShootBowEvent.getForce()));
            Enchantment infinityEnchant = bow.getEnchantment(Enchantment.ID_BOW_INFINITY);
            boolean infinity = infinityEnchant != null && infinityEnchant.getLevel() > 0;
            if (infinity && projectile instanceof EntityArrow) {
                ((EntityArrow) projectile).setPickupMode(EntityProjectile.PICKUP_NONE);
            }

            if (entityShootBowEvent.getProjectile() != null) {
                ProjectileLaunchEvent projectev = new ProjectileLaunchEvent(entityShootBowEvent.getProjectile());
                Server.getInstance().getPluginManager().callEvent(projectev);
                if (projectev.isCancelled()) {
                    projectile.kill();
                } else {
                    projectile.spawnToAll();
                    entity.getLevel().addSound(entity, Sound.RANDOM_BOW);
                }
            }
        }
    }

    private static void playBowAnimation(IEntity ientity, Entity target) {
        Entity entity = ientity.getEntity();
        entity.setDataProperty(new LongEntityData(Entity.DATA_TARGET_EID, target.getId()));
        entity.setDataFlag(Entity.DATA_FLAGS, Entity.DATA_FLAG_FACING_TARGET_TO_RANGE_ATTACK);
    }

    private static void stopBowAnimation(IEntity ientity) {
        Entity entity = ientity.getEntity();
        entity.setDataProperty(new LongEntityData(Entity.DATA_TARGET_EID, 0L));
        entity.setDataFlag(Entity.DATA_FLAGS, Entity.DATA_FLAG_FACING_TARGET_TO_RANGE_ATTACK, false);
    }
}