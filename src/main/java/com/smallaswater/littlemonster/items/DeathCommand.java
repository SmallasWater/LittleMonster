package com.smallaswater.littlemonster.items;

import java.util.Map;

/**
 * @author 若水
 */
public class DeathCommand extends BaseItem{

    private String cmd;

    public DeathCommand(Map map){
        super((int) map.get("round"));
        cmd = (String) map.get("cmd");
    }

    public String getCmd() {
        return cmd;
    }
}
