package com.smallaswater.littlemonster.entity.baselib;

import cn.nukkit.math.AxisAlignedBB;

/**
 * @author SmallasWater
 * 2022/1/31
 */
public class Area extends AxisAlignedBB {

    private double minX;

    private double maxX;

    private double minY;

    private double maxY;

    private double minZ;

    private double maxZ;

    public Area(double minX,
                double maxX,
                double minY,
                double maxY,
                double minZ,
                double maxZ){
        super(minX,minY,minZ,maxX,maxY,maxZ);
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    @Override
    public double getMinX() {
        return minX;
    }

    @Override
    public double getMinY() {
        return minY;
    }

    @Override
    public double getMinZ() {
        return minZ;
    }

    @Override
    public double getMaxX() {
        return maxX;
    }

    @Override
    public double getMaxY() {
        return maxY;
    }

    @Override
    public double getMaxZ() {
        return maxZ;
    }

    @Override
    public AxisAlignedBB clone() {
        return (AxisAlignedBB) super.clone();
    }

}
