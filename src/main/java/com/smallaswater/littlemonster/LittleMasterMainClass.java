package com.smallaswater.littlemonster;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import com.smallaswater.littlemonster.commands.LittleNpcSpawnCommand;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.config.PositionConfig;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.events.LittleMasterListener;
import com.smallaswater.littlemonster.manager.PlayerFlotTextManager;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import com.smallaswater.littlemonster.threads.PluginMasterThreadPool;
import com.smallaswater.littlemonster.threads.runnables.AutoSpawnTask;
import com.smallaswater.littlemonster.threads.runnables.FlotTask;
import com.smallaswater.littlemonster.threads.runnables.SpawnMonsterTask;
import com.smallaswater.littlemonster.threads.runnables.TimmerRunnable;
import com.smallaswater.littlemonster.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author SmallasWater
 * Create on 2021/6/28 7:48
 * Package com.smallaswater.littlemaster
 */
public class LittleMasterMainClass extends PluginBase {

    public Config nbtItem;

    private static LittleMasterMainClass masterMainClass;

    public static LinkedHashMap<String, Skin> loadSkins = new LinkedHashMap<>();

    public LinkedHashMap<String, MonsterConfig> monsters = new LinkedHashMap<>();

    public LinkedHashMap<String, PositionConfig> positions = new LinkedHashMap<>();

    public HashMap<String, Integer> time = new HashMap<>();

    public ArrayList<PlayerFlotTextManager> texts = new ArrayList<>();

    private static final String[] SKINS = {"粉蓝双瞳猫耳少女","小丸子","小埋","小黑苦力怕","尸鬼","拉姆","熊孩子","狂三","米奇","考拉","黑岩射手"};

    @Override
    public void onEnable() {
        masterMainClass = this;
        //检查api

        Entity.registerEntity("LittleNpc", LittleNpc.class);
        BaseSkillManager.initSkill();
        this.getServer().getPluginManager().registerEvents(new LittleMasterListener(),this);
        init();
        saveDefaultConfig();
        reloadConfig();
        this.saveResource("LittleMaster介绍.pdf",true);
        this.getServer().getCommandMap().register("lt",new LittleNpcSpawnCommand());
        this.getLogger().info("副本信息读取成功");

        int spawnDelay = this.getConfig().getInt("npcs.autospawn-tick", 0);
        if (spawnDelay > 0) {
            this.getServer().getScheduler().scheduleDelayedRepeatingTask(this, new AutoSpawnTask(this), spawnDelay, spawnDelay);
        }
        PluginMasterThreadPool.executeThread(new FlotTask());
        PluginMasterThreadPool.executeThread(new SpawnMonsterTask());
        PluginMasterThreadPool.executeThread(new TimmerRunnable());
    }

    public void init(){
        loadDefaultSkin();
        this.getLogger().info("开始读取皮肤");
        loadSkin();
        this.getLogger().info("正在读取副本信息");
        nbtItem = new Config(this.getDataFolder()+"/nbtItem.yml",Config.YAML);
        monsters = new LinkedHashMap<>();
        positions = new LinkedHashMap<>();
        for(String name:Utils.getDefaultFiles("Monster")){
            this.getLogger().info("读取 "+name+".yml");
            MonsterConfig config = MonsterConfig.loadEntity(name,new Config(this.getDataFolder()+"/Monster/"+name+".yml",Config.YAML));
            if(config != null){
                monsters.put(name,config);
            }else{
                this.getLogger().warning(name+"怪物数据读取失败");
            }
        }
        for(String name:Utils.getDefaultFiles("Position")){
            this.getLogger().info("读取 "+name+".yml");
            PositionConfig config = PositionConfig.loadPosition(name,new Config(this.getDataFolder()+"/Position/"+name+".yml",Config.YAML));
            if(config != null){
                positions.put(name,config);
            }else{
                this.getLogger().warning(name+"刷怪点数据读取失败");
            }
        }
    }
    public static LittleMasterMainClass getMasterMainClass() {
        return masterMainClass;
    }

    private void loadDefaultSkin() {
        if(!new File(this.getDataFolder()+"/Skins").exists()){
            this.getLogger().info("未检测到Skins文件夹，正在创建");
            if(!new File(this.getDataFolder()+"/Skins").mkdirs()){
                this.getLogger().info("Skins文件夹创建失败");
            }else{
                this.getLogger().info("Skins 文件夹创建完成，正在载入预设皮肤");
                initSkin();
            }
        }
    }
    private void initSkin(){
        for(String s:SKINS){
            if(!new File(this.getDataFolder()+"/Skins/"+s).exists()){
                if(!new File(this.getDataFolder()+"/Skins/"+s).mkdirs()){
                    this.getLogger().info("载入 "+s+"失败");
                    continue;
                }
            }
            if(this.getResource("skin/"+s+"/skin.json") != null) {
                this.saveResource("skin/" + s + "/skin.json", "/Skins/" + s + "/skin.json", false);
            }
            this.saveResource("skin/"+s+"/skin.png","/Skins/"+s+"/skin.png",false);
            this.getLogger().info("成功载入 "+s+"皮肤");
        }

    }
    private void loadSkin() {
        if(!new File(this.getDataFolder()+"/Skins").exists()){
            initSkin();
        }
        File[] files = new File(this.getDataFolder()+"/Skins").listFiles();
        if(files != null && files.length > 0){
            for(File file:files){
                String skinName = file.getName();
                if(new File(this.getDataFolder()+"/Skins/"+skinName+"/skin.png").exists()){
                    Skin skin = new Skin();
                    BufferedImage skindata = null;
                    try {
                        skindata = ImageIO.read(new File(this.getDataFolder()+"/Skins/"+skinName+"/skin.png"));
                    } catch (IOException var19) {
                        System.out.println("不存在皮肤");
                    }

                    if (skindata != null) {
                        skin.setSkinData(skindata);
                        skin.setSkinId(skinName);
                    }
                    //如果是4D皮肤
                    if(new File(this.getDataFolder()+"/Skins/"+skinName+"/skin.json").exists()){
                        Map<String, Object> skinJson = (new Config(this.getDataFolder()+"/Skins/"+skinName+"/skin.json", Config.JSON)).getAll();
                        String geometryName = null;

                        if(skinJson.containsKey("format_version")){
                            skin.generateSkinId("littlemaster");
                            for(Map.Entry<String, Object> entry1: skinJson.entrySet()){
                                if(geometryName == null){
                                    if(entry1.getKey().startsWith("geometry")) {
                                        geometryName = entry1.getKey();
                                    }
                                }
                            }
                            skin.setSkinResourcePatch("{\"geometry\":{\"default\":\""+geometryName+"\"}}");
                            skin.setGeometryData(Utils.readFile(new File(this.getDataFolder()+"/Skins/"+skinName+"/skin.json")));
                            skin.setTrusted(true);
                        }else{
                            for (Map.Entry<String, Object> entry1: skinJson.entrySet()){
                                if(geometryName == null){
                                    geometryName = entry1.getKey();
                                }
                            }
                            skin.setGeometryName(geometryName);
                            skin.setGeometryData(Utils.readFile(new File(this.getDataFolder()+"/Skins/"+skinName+"/skin.json")));
                        }

                    }
                    this.getLogger().info(skinName+"皮肤读取完成");
                    loadSkins.put(skinName,skin);
                }else{
                    this.getLogger().info("错误的皮肤名称格式 请将皮肤文件命名为 skin.png");
                }
            }
        }
    }
}
