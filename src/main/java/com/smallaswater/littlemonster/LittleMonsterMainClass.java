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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author SmallasWater
 * Create on 2021/6/28 7:48
 * Package com.smallaswater.littlemaster
 */
public class LittleMonsterMainClass extends PluginBase {

    public static boolean debug = false;

    public Config nbtItem;

    private static LittleMonsterMainClass masterMainClass;

    public static LinkedHashMap<String, Skin> loadSkins = new LinkedHashMap<>();

    public LinkedHashMap<String, MonsterConfig> monsters = new LinkedHashMap<>();

    public LinkedHashMap<String, PositionConfig> positions = new LinkedHashMap<>();

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
        this.saveResource("LittleMonster介绍.pdf",true);

        if (this.getConfig().getBoolean("debug", false)) {
            debug = true;
            this.getLogger().warning("§c=========================================");
            this.getLogger().warning("§c 警告：您开启了debug模式！");
            this.getLogger().warning("§c Warning: You have turned on debug mode!");
            this.getLogger().warning("§c=========================================");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {

            }
        }

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

    public static LittleMonsterMainClass getMasterMainClass() {
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
                    File skinJsonFile = new File(this.getDataFolder() + "/Skins/" + skinName + "/skin.json");
                    if(skinJsonFile.exists()){
                        Map<String, Object> skinJson = (new Config(this.getDataFolder()+"/Skins/"+skinName+"/skin.json", Config.JSON)).getAll();
                        String geometryName = null;

                        String formatVersion = (String) skinJson.getOrDefault("format_version", "1.10.0");
                        skin.setGeometryDataEngineVersion(formatVersion); //设置皮肤版本，主流格式有1.16.0,1.12.0(Blockbench新模型),1.10.0(Blockbench Legacy模型),1.8.0
                        switch (formatVersion){
                            case "1.16.0":
                            case "1.12.0":
                                geometryName = getGeometryName(skinJsonFile);
                                if(geometryName.equals("nullvalue")){
                                    this.getLogger().error("LittleMonster 暂不支持" + skinName + "皮肤所用格式！请等待更新！");
                                }else{
                                    skin.generateSkinId(skinName);
                                    skin.setSkinResourcePatch("{\"geometry\":{\"default\":\"" + geometryName + "\"}}");
                                    skin.setGeometryName(geometryName);
                                    skin.setGeometryData(Utils.readFile(skinJsonFile));
                                    this.getLogger().info("皮肤 " + skinName + " 读取中");
                                }
                                break;
                            default:
                                this.getLogger().warning("["+skinJsonFile.getName()+"] 的版本格式为："+formatVersion + "，正在尝试加载！");
                            case "1.10.0":
                            case "1.8.0":
                                for (Map.Entry<String, Object> entry : skinJson.entrySet()) {
                                    if (geometryName == null) {
                                        if (entry.getKey().startsWith("geometry")) {
                                            geometryName = entry.getKey();
                                        }
                                    }else {
                                        break;
                                    }
                                }
                                skin.generateSkinId(skinName);
                                skin.setSkinResourcePatch("{\"geometry\":{\"default\":\"" + geometryName + "\"}}");
                                skin.setGeometryName(geometryName);
                                skin.setGeometryData(Utils.readFile(skinJsonFile));
                                break;
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

    public String getGeometryName(File file) {
        Config originGeometry = new Config(file, Config.JSON);
        if (!originGeometry.getString("format_version").equals("1.12.0") && !originGeometry.getString("format_version").equals("1.16.0")) {
            return "nullvalue";
        }
        //先读取minecraft:geometry下面的项目
        List<Map<String, Object>> geometryList = (List<Map<String, Object>>) originGeometry.get("minecraft:geometry");
        //不知道为何这里改成了数组，所以按照示例文件读取第一项
        Map<String, Object> geometryMain = geometryList.get(0);
        //获取description内的所有
        Map<String, Object> descriptions = (Map<String, Object>) geometryMain.get("description");
        return (String) descriptions.getOrDefault("identifier", "geometry.unknown"); //获取identifier
    }

}
