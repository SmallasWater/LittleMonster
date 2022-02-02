package com.smallaswater.littlemonster.threads.runnables;

import com.smallaswater.littlemonster.LittleMasterMainClass;

/**
 * @author SmallasWater
 * Create on 2021/6/29 8:17
 * Package com.smallaswater.littlemonster.threads.runnables
 */
public abstract class BasePluginThreadTask implements Runnable {

    @Override
    public final void run() {
        while (LittleMasterMainClass.getMasterMainClass().isEnabled()) {
            if(!scheduler()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * 循环执行
     * @return 是否终止
     * */
    abstract public boolean scheduler();
}
