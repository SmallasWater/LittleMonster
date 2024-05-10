package com.smallaswater.littlemonster.entity.vanilla;

import cn.nukkit.block.*;
import cn.nukkit.level.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Pathfinder {
    private static final Random random = new Random();

    public static boolean isCanMove(Position pos) {
        Block y1 = pos.add(0, -1).getLevelBlock();
        Block y2 = pos.getLevelBlock();
        Block y3 = pos.add(0, 1).getLevelBlock();
        Block y4 = pos.add(0, 2).getLevelBlock();

        Block[] list = {y1, y2, y3, y4};
        Boolean[] res = {y1.isSolid(), y2.isSolid(), y3.isSolid(), y4.isSolid()};

        if (y1 instanceof BlockFence || y1 instanceof BlockWall) {
            // 若y1为 栅栏、墙 则设置y2的res为true
            res[1] = true;
        }
        for (int i = 0; i < list.length; i++) {
            if (list[i] instanceof BlockDoor || list[i] instanceof BlockTrapdoor) {
                // 若y1为 门、活板门 则设置为false
                res[i] = false;
            }
        }
        if (res[0] && !res[1] && !res[2]) {
            return true;
        }
        return false;
    }

    public static List<Position> getNextPosList(Position p) {
        List<Position> arr = new ArrayList<>();
        arr.add(p.add(-1, 0, 0));
        arr.add(p.add(1, 0, 0));
        arr.add(p.add(0, 0, -1));
        arr.add(p.add(0, 0, 1));
        arr.add(p.add(-1, 1, 0));
        arr.add(p.add(1, 1, 0));
        arr.add(p.add(0, 1, -1));
        arr.add(p.add(0, 1, 1));
        arr.add(p.add(-1, -1, 0));
        arr.add(p.add(1, -1, 0));
        arr.add(p.add(0, -1, -1));
        arr.add(p.add(0, -1, 1));
        return arr;
    }

    public static Position getParticlePos(Position pos) {
        return pos.floor().add(0.5, 0, 0.5);
    }

    public static int getEstimateCost(Position pos1, Position pos2) {
        return (int) (Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y - pos2.y) + Math.abs(pos1.z - pos2.z));
    }
    public static Position getNode(Position start, int length) {
        return getNode(start, length, length * 3);
    }

    public static Position getNode(Position start, int length, int maxStep) {
        List<String> visitedNode = new ArrayList<>();
        visitedNode.add(getStringPos(start));
        List<Position> nextPosList = getNextPosList(start);
        List<Position> moveNode = new ArrayList<>();
        moveNode.add(start);
        int n = 0;
        boolean isEnd = false;
        Position endPos = null;

        while (maxStep > 0) {
            n++;
            int cost = 0;
            Position costPosition = null;
            List<Position> tempNode = new ArrayList<>();

            for (Position pos : nextPosList) {
                String posString = getStringPos(pos);
                if (visitedNode.contains(posString)) continue;
                if (isCanMove(pos)) {
                    tempNode.add(pos);
                }
            }

            if (tempNode.isEmpty()) {
                // No valid next positions
                moveNode.remove(moveNode.size() - 1);
                if (moveNode.size() > 1) {
                    n -= 2;
                    nextPosList = getNextPosList(moveNode.get(moveNode.size() - 1));
                } else {
                    // Failed, unable to reach the expected length
                    break;
                }
            } else {
                Position pos;
                if (tempNode.size() == 1) {
                    pos = tempNode.get(0);
                    cost = getEstimateCost(start, pos);
                    costPosition = pos;
                } else {
                    pos = tempNode.get(random.nextInt(tempNode.size()));
                    cost = getEstimateCost(start, pos);
                    costPosition = pos;
                }

                if (cost > length) {
                    isEnd = true;
                    if (cost > (endPos == null ? 0 : getEstimateCost(start, endPos))) {
                        endPos = pos;
                    }
                } else if (endPos == null || cost > getEstimateCost(start, endPos)) {
                    endPos = pos;
                }

                visitedNode.add(getStringPos(pos));
                moveNode.add(pos);
                if (isEnd) {
                    break;
                }
                nextPosList = getNextPosList(pos);
            }

            maxStep--;
        }

        return endPos != null ? getParticlePos(endPos) : null;
    }

    private static String getStringPos(Position pos) {
        return pos.x + " " + pos.y + " " + pos.z;
    }
}
