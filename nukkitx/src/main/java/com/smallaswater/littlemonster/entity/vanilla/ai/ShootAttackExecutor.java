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
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Sound;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.entity.IEntity;

public class ShootAttackExecutor {
    //protected float speed;
    //protected int maxShootDistanceSquared;
    //protected int coolDownTick;
    public boolean inExecute;
    public int failedAttackCount = 0;// 攻击失败次数

    //protected float damage;
    public boolean execute(IEntity ientity, Entity target) {
        inExecute = true;
        if (ientity.isVanillaEntity()) {
            //VanillaNPC vanillaNPC = (VanillaNPC) ientity.getEntity();
            //this.speed = (float) vanillaNPC.speed;
            //this.maxShootDistanceSquared = (int) (vanillaNPC.getConfig().getAttackDistance() * vanillaNPC.getConfig().getAttackDistance());
            //this.coolDownTick = vanillaNPC.getAttackSleepTick();
            //this.damage = vanillaNPC.getDamage();
            playBowAnimation(ientity, target);
            LittleMonsterMainClass.getInstance().getServer().getScheduler().scheduleDelayedTask(LittleMonsterMainClass.getInstance(), ()->{
                inExecute = false;
                bowShoot(ientity, target);
                stopBowAnimation(ientity);
            }, 40);
        } else {
            throw new RuntimeException("Not support Entity type.");
        }

        return true;
    }

    protected void bowShoot(IEntity ientity, Entity target) {
        // 1. 安全检查：如果目标消失或死亡，停止射击
        if (target == null || !target.isAlive() || !target.isValid()) {
            return;
        }

        ItemBow bow = (ItemBow) Item.get(261);
        EntityLiving entity = (EntityLiving) ientity.getEntity();

        // --- 实时瞄准计算 ---

        // 获取起止点位置
        // 修正1: 骷髅射箭应该从"眼睛高度"发出，而不是脚底或身体中间
        double startX = entity.x;
        double startY = entity.y + entity.getEyeHeight();
        double startZ = entity.z;

        // 修正2: 瞄准玩家的"身体中心" (eyeHeight - 0.5 左右)，而不是脚底，也不是头顶
        double targetX = target.x;
        double targetY = target.y + target.getEyeHeight() - 0.2; // 稍微瞄准脖子/胸口位置
        double targetZ = target.z;

        // 计算距离差
        double dx = targetX - startX;
        double dy = targetY - startY;
        double dz = targetZ - startZ;
        double hDistance = Math.sqrt(dx * dx + dz * dz); // 水平距离

        // 计算 Yaw (水平朝向)
        // Nukkit 数学公式: atan2(dz, dx) * 180 / PI - 90
        double yaw = Math.atan2(dz, dx) * 180 / Math.PI - 90;

        // 计算 Pitch (垂直朝向)
        // 修正3: 重力补偿。距离越远，枪口要抬得越高。
        // 力度为 3.0 时，箭矢下坠较小，但远距离仍需微调。
        // 简单算法：每 10 格距离抬高约 1~2 度
        double gravityPitch = hDistance * 0.15;
        double pitch = -Math.atan2(dy, hDistance) * 180 / Math.PI;
        pitch -= gravityPitch; // 减去 pitch 值意味着向上抬起

        // 强制更新服务端骷髅的朝向，这样发包给客户端时，骷髅也是看着玩家的
        entity.setRotation(yaw, pitch);

        // --- NBT 构建 ---
        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<DoubleTag>("Pos")
                        .add(new DoubleTag("", startX))
                        .add(new DoubleTag("", startY))
                        .add(new DoubleTag("", startZ)))
                .putList(new ListTag<DoubleTag>("Motion")
                        // 使用刚刚计算好的 yaw 和 pitch 生成向量，确保指哪打哪
                        .add(new DoubleTag("", -Math.sin(yaw / 180 * Math.PI) * Math.cos(pitch / 180 * Math.PI)))
                        .add(new DoubleTag("", -Math.sin(pitch / 180 * Math.PI)))
                        .add(new DoubleTag("", Math.cos(yaw / 180 * Math.PI) * Math.cos(pitch / 180 * Math.PI))))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", (yaw > 180 ? 360 : 0) - (float) yaw))
                        .add(new FloatTag("", (float) -pitch)))
                .putDouble("damage", ientity.getConfig().getDamage());

        double f = 3.0; // 满弦力度

        EntityArrow arrow = (EntityArrow) Entity.createEntity("Arrow", entity.chunk, nbt, entity, false);

        arrow.age = 300;
        LittleMonsterMainClass.getInstance().getServer().getScheduler().scheduleDelayedTask(LittleMonsterMainClass.getInstance(), ()->{
            if (arrow.onGround) {
                failedAttackCount++;
            } else if (arrow.hadCollision) {
                failedAttackCount = 0;
            }
        }, 30);

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
                ((EntityArrow) projectile).setPickupMode(EntityProjectile.PICKUP_NONE_REMOVE);
            }

            if (entityShootBowEvent.getProjectile() != null) {
                ProjectileLaunchEvent projected = new ProjectileLaunchEvent(entityShootBowEvent.getProjectile());
                Server.getInstance().getPluginManager().callEvent(projected);
                if (projected.isCancelled()) {
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