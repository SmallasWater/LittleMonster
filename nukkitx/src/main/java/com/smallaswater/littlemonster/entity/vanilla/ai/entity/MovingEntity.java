package com.smallaswater.littlemonster.entity.vanilla.ai.entity;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import com.smallaswater.littlemonster.entity.vanilla.ai.route.AdvancedRouteFinder;
import com.smallaswater.littlemonster.entity.vanilla.ai.route.Node;
import com.smallaswater.littlemonster.entity.vanilla.ai.route.RouteFinder;

abstract public class MovingEntity extends EntityHuman {
    private boolean isKnockback = false;
    public RouteFinder route = null;
    private Vector3 target = null;
    public boolean autoSeeFont = true;
    private int jammingTick = 0;
    private long passedTick = 1;

    public MovingEntity(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.route = new AdvancedRouteFinder(this);
    }

    public void jump() {
        if (this.onGround) {
            this.motionY += 0.35;
        }
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (this.closed) {
            return false;
        }
        passedTick++;

        boolean hasUpdate = super.entityBaseTick(tickDiff);

        if (this.isKnockback) {                   // knockback 이 true 인 경우는 맞은 직후
            this.isKnockback = false;
        } else if (this.onGround) {
            this.motionX = this.motionZ = 0;
        }
        this.motionY = 0;

        this.motionX *= (1 - this.getDrag());
        this.motionZ *= (1 - this.getDrag());
        if (this.motionX < 0.001 && this.motionX > -0.001) this.motionX = 0;
        if (this.motionZ < 0.001 && this.motionZ > -0.001) this.motionZ = 0;

        if (this.onGround && this.hasSetTarget() && (this.route.getDestination() == null || this.route.getDestination().distance(this.getTarget()) > 2)) { //目标移动
            this.route.setPositions(this.level, this, this.getTarget(), this.boundingBox);

            if (this.route.isSearching()) this.route.research();
            else this.route.search();

            hasUpdate = true;
        }

        if (this.route != null && !this.route.isSearching() && this.route.isSuccess() && this.route.hasRoute()) { // entity has route to go
            hasUpdate = true;
            Node node = this.route.get();
            if (node != null) {
                Vector3 vec = node.getVector3();
                double diffX = Math.pow(vec.x - this.x, 2);
                double diffZ = Math.pow(vec.z - this.z, 2);
                if (diffX + diffZ == 0) {
                    jammingTick = 0;
                    if (this.route.next() == null) {
                        this.route.arrived();
                    }
                } else {
                    jammingTick++;
                    int negX = vec.x - this.x < 0 ? -1 : 1;
                    int negZ = vec.z - this.z < 0 ? -1 : 1;

                    this.motionX = Math.min(Math.abs(vec.x - this.x), diffX / (diffX + diffZ) * this.getMovementSpeed()) * negX;
                    this.motionZ = Math.min(Math.abs(vec.z - this.z), diffZ / (diffX + diffZ) * this.getMovementSpeed()) * negZ;

                    if (vec.y > this.y) this.motionY += this.getGravity();

                    if (this.autoSeeFont) {
                        double angle = Math.atan2(vec.z - this.z, vec.x - this.x);
                        this.yaw = (float) ((angle * 180) / Math.PI) - 90;
                    }
                }
            }
        }

        //this.move(this.motionX, this.motionY, this.motionZ);

        this.checkGround();
        if (!this.onGround) {
            this.motionY -= this.getGravity();
            hasUpdate = true;
        }


        if ((this.motionX != 0 || this.motionZ != 0) && this.isCollidedHorizontally) {
            this.jump();
        }

        this.x += this.motionX;
        if (Math.abs(this.y + this.motionY - (int) this.y) < this.getGravity() - 0.01) {
            this.y = (int) this.y;
        } else {
            this.y += this.motionY;
        }
        this.z += this.motionZ;

        AxisAlignedBB bb = this.getBoundingBox();
        final double x = this.getX(), y = this.getY(), z = this.getZ();
        final float dx = this.getWidth() / 2, dy = this.getLength() , dz = this.getHeight() / 2;
        bb.setMaxX(x + dx);
        bb.setMinX(x - dx);
        bb.setMaxY(y + dy);
        bb.setMinY(y);
        bb.setMaxZ(z + dz);
        bb.setMinZ(z - dz);

        return hasUpdate;
    }

    private double toRound(double i) {
        long p = Math.round(i);
        if (Math.abs(i - p) < 0.2) {
            return p;
        } else {
            return i;
        }
    }

    public double getRange() {
        return 30.0;
    }

    public void setTarget(Vector3 vec) {
        this.setTarget(vec, false);
    }

    public void setTarget(Vector3 vec, boolean forceSearch) {

        if (forceSearch || !this.hasSetTarget()) {
            this.target = vec;
        }

        if (this.hasSetTarget() && (forceSearch || !this.route.hasRoute())) {
            this.route.setPositions(this.level, this, this.target, this.boundingBox.clone());
            if (this.route.isSearching()) this.route.research();
            else this.route.search();
        }
    }

    public Vector3 getTarget() {
        return new Vector3(this.target.x, this.target.y, this.target.z);
    }

    /**
     * Returns whether the entity has following target
     * Entity will try to move to position where target exists
     */
    public boolean hasFollowingTarget() {
        return this.route.getDestination() != null && this.target != null && this.distance(this.target) < this.getRange();
    }

    /**
     * Returns whether the entity has set its target
     * The entity may not follow the target if there is following target and set target is different
     * If following distance of target is too far to follow or cannot reach, set target will be the next following target
     */
    public boolean hasSetTarget() {
        return this.target != null && this.distance(this.target) < this.getRange();
    }

    @Override
    protected void checkGroundState(double movX, double movY, double movZ, double dx, double dy, double dz) {
        this.isCollidedVertically = movY != dy;
        this.isCollidedHorizontally = (movX != dx || movZ != dz);
        this.isCollided = (this.isCollidedHorizontally || this.isCollidedVertically);

        // this.onGround = (movY != dy && movY < 0);
        // onGround 는 onUpdate 에서 확인
    }

    private void _checkGround() {
        AxisAlignedBB[] list = this.level.getCollisionCubes(this, this.level.getTickRate() > 1 ? this.boundingBox.getOffsetBoundingBox(0, -1, 0) : this.boundingBox.addCoord(0, -1, 0), false);
        double maxY = 0;
        for (AxisAlignedBB bb : list) {
            if (bb.getMaxY() > maxY) {
                maxY = bb.getMaxY();
            }
        }
        this.onGround = (maxY == this.boundingBox.getMinY());
    }

    private void checkGround() {
        Block block = this.level.getBlock((int) this.x, (int) (this.y - this.getGravity()), (int) this.z - 1);
        this.onGround = !AdvancedRouteFinder.canPassThroughBlock(block);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
    }

    @Override
    public void knockBack(Entity attacker, double damage, double x, double z, double base) {
        this.isKnockback = true;
        super.knockBack(attacker, damage, x, z, base);
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        this.level.addEntityMovement(this, x, y, z, yaw, pitch, headYaw);
    }
}
