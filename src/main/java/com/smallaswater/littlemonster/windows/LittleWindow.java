package com.smallaswater.littlemonster.windows;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.manager.KeyHandleManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * @author SmallasWater
 * Create on 2021/6/30 14:40
 * Package com.smallaswater.littlemonster.windows
 */
public class LittleWindow {

    private static final int MENU = 0x55;

    public static LinkedHashMap<Integer,FormWindow> WINDOWS = new LinkedHashMap<>();

    public static void sendMenu(Player player){
        FormWindowSimple simple = new FormWindowSimple("副本主页","");
        for(String name: LittleMonsterMainClass.getInstance().monsters.keySet()){
            simple.addButton(new ElementButton(name,new ElementButtonImageData("path","textures/ui/bad_omen_effect")));
        }
        if(simple.getButtons().size() == 0){
            simple.setContent("无任何副本信息");
        }
        player.showFormWindow(simple,MENU + 1);
        WINDOWS.put(MENU + 1,simple);
    }

    public static void sendSetting(Player player){
        Object o = KeyHandleManager.getKey(player,"menu");
        if(o != null){
            MonsterConfig config;
            if(LittleMonsterMainClass.getInstance().monsters.containsKey(o.toString())){
                config = LittleMonsterMainClass.getInstance().monsters.get(o.toString());
                FormWindowCustom custom = new FormWindowCustom("副本设置");
                custom.addElement(new ElementInput("请输入血量","整数血量",config.getHealth()+""));
                custom.addElement(new ElementInput("请输入攻击力","整数攻击力",config.getDamage()+""));
                custom.addElement(new ElementInput("请输入大小","小数大小",config.getSize()+""));
                custom.addElement(new ElementInput("请输入移动速度","小数移动速度",config.getMoveSpeed()+""));
                custom.addElement(new ElementDropdown("请选择皮肤",new ArrayList<>( LittleMonsterMainClass.loadSkins.keySet())));
                player.showFormWindow(custom,MENU + 2);
                WINDOWS.put(MENU + 2,custom);
            }
        }
    }
}
