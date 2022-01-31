package com.smallaswater.littlemonster.utils;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.Config;
import com.smallaswater.littlemonster.LittleMasterMainClass;
import com.smallaswater.littlemonster.entity.LittleNpc;
import com.smallaswater.littlemonster.entity.baselib.Area;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * @author SmallasWater
 * Create on 2021/6/28 8:39
 * Package com.smallaswater.littlemonster.utils
 */
public class Utils {

    private static final SplittableRandom random = new SplittableRandom(System.currentTimeMillis());

    public static final int ACCORDING_X_OBTAIN_Y = 0;

    public static final int ACCORDING_Y_OBTAIN_X = 1;

    public static int rand(int min, int max) {
        return min == max ? max : random.nextInt(max + 1 - min) + min;
    }

    public static double rand(double min, double max) {
        return min == max ? max : min + Math.random() * (max - min);
    }

    public static float rand(float min, float max) {
        return min == max ? max : min + (float)Math.random() * (max - min);
    }

    public static boolean rand() {
        return random.nextBoolean();
    }

    public static double calLinearFunction(Vector3 pos1, Vector3 pos2, double element, int type) {
        if (pos1.getFloorY() != pos2.getFloorY()) {
            return 1.7976931348623157E308D;
        } else if (pos1.getX() == pos2.getX()) {
            return type == 1 ? pos1.getX() : 1.7976931348623157E308D;
        } else if (pos1.getZ() == pos2.getZ()) {
            return type == 0 ? pos1.getZ() : 1.7976931348623157E308D;
        } else {
            return type == 0 ? (element - pos1.getX()) * (pos1.getZ() - pos2.getZ()) / (pos1.getX() - pos2.getX()) + pos1.getZ() : (element - pos1.getZ()) * (pos1.getX() - pos2.getX()) / (pos1.getZ() - pos2.getZ()) + pos1.getX();
        }
    }

    public static String readFile(File file){
        String content = "";
        try{
            content = cn.nukkit.utils.Utils.readFile(file);
        }catch (IOException e){
            e.printStackTrace();
        }
        return content;
    }

    public static ArrayList<Player> getAroundOfPlayers(Entity player, int size) {
        ArrayList<Player> players = new ArrayList<>();
        for(Entity entity:getAroundPlayers(player,size,true,false,false)){
            players.add((Player) entity);
        }
        return players;
    }

    public static boolean canAttackNpc(LittleNpc l1,LittleNpc l2){
        if(l1.getConfig() == null){
            l1.setConfig(LittleMasterMainClass.getMasterMainClass().monsters.get(l1.getName()));
        }
        if(l2.getConfig() == null){
            l2.setConfig(LittleMasterMainClass.getMasterMainClass().monsters.get(l2.getName()));
        }

        try{
            if(l1.getConfig()
                    .getCampName()
                    .equalsIgnoreCase(l2
                            .getConfig()
                            .getCampName())){
                return l1.getConfig().isCanAttackSameCamp();
            }else {
                return l1.getConfig().getDamageCamp().contains(l2.getConfig().getCampName());
            }

        }catch (Exception e){
            Server.getInstance().getLogger().error("NPC配置 空！l1 config: "+l1.getConfig()+" l2 config: "+l2.getConfig(), e);
            return false;
        }

    }

    public static LinkedList<Entity> getAroundPlayers(Entity player, int size,boolean isPlayer, boolean isEntity,boolean isNpc) {
        LinkedList<Entity> explodePlayer = new LinkedList<>();
        for(Entity player1: player.level.getNearbyEntities(new Area(
                player.x - size / 2.0,
                player.x + size / 2.0,
                player.y - size / 2.0,
                player.y + size / 2.0,
                player.z - size / 2.0,
                player.z + size / 2.0),player,true)){
            if(isPlayer && player1 instanceof Player){
                explodePlayer.add(player1);
                continue;
            }
            if(isEntity){
                if(isNpc && player1 instanceof LittleNpc){
                    if(player instanceof LittleNpc){
                        if(canAttackNpc((LittleNpc) player,(LittleNpc)player1)){
                            explodePlayer.add(player1);
                        }
                    }
                }else if(player1 instanceof EntityLiving &&!(player1 instanceof EntityHuman) && !player1.isImmobile()){
                    explodePlayer.add(player1);
                }
            }
        }
        return explodePlayer;
    }

    public static void saveNbt(String name, String text){
        Config config = getNbtItemConfig();
        config.set(name,text);
        config.save();
    }

    public static LinkedList<Effect> effectFromString(List<String> s){
        LinkedList<Effect> effects = new LinkedList<>();
        for(String e:s){
            String[] eff = e.split(":");
            effects.add(Effect.getEffect(Integer.parseInt(eff[0]))
                    .setAmplifier(Integer.parseInt(eff[1])).setDuration(Integer.parseInt(eff[2])*20));
        }
        return effects;
    }

    public static ArrayList<LittleNpc> getEntitys(String name){
        ArrayList<LittleNpc> littleNpcs = new ArrayList<>();
        for(Level level: Server.getInstance().getLevels().values()) {
            for (Entity entitys : level.getEntities()) {
                if (entitys instanceof LittleNpc && ((LittleNpc) entitys).name.equalsIgnoreCase(name)) {
                    littleNpcs.add((LittleNpc) entitys);
                }
            }
        }
        return littleNpcs;
    }

    public static int getEntityCount(Level level, String entityName,String pos){
        int count = 0;
        for(Entity entitys: level.getEntities()){
            String name = getMonster(entitys);
            if(name != null && name.equals(entityName)){
                if(entitys instanceof LittleNpc){
                    if(((LittleNpc) entitys).spawnPos != null){
                        if(((LittleNpc) entitys).spawnPos.equalsIgnoreCase(pos)){
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    public static boolean isMonster(Entity entity) {
        CompoundTag tag = entity.namedTag;
        return tag.contains(LittleNpc.TAG);
    }

    public static boolean positionEqual(Position p1,Position p2){
        return p1.getFloorX() == p2.getFloorX() && p1.getFloorY() == p2.getFloorY()
                && p1.getFloorZ() == p2.getFloorZ() && p1.level.getFolderName().equalsIgnoreCase(p2.level.getFolderName());
    }

    public static String getMonster(Entity entity) {
        if(isMonster(entity)){
            CompoundTag tag = entity.namedTag;
            return tag.getString(LittleNpc.TAG);
        }
        return null;
    }


    public static String[] getDefaultFiles(String fileName) {
        List<String> names = new ArrayList<>();
        File files = new File(LittleMasterMainClass.getMasterMainClass().getDataFolder()+ "/"+fileName);
        if(files.isDirectory()){
            File[] filesArray = files.listFiles();
            if(filesArray != null){
                if(filesArray.length>0){
                    for(File file : filesArray){
                        names.add( file.getName().substring(0, file.getName().lastIndexOf(".")));
                    }
                }
            }
        }
        return names.toArray(new String[0]);
    }

    public static Config getNbtItemConfig(){
        if(LittleMasterMainClass.getMasterMainClass().nbtItem == null){
            LittleMasterMainClass.getMasterMainClass().nbtItem = new Config(LittleMasterMainClass.getMasterMainClass().getDataFolder()+"/nbtitem.yml",Config.YAML);
        }
        return LittleMasterMainClass.getMasterMainClass().nbtItem;
    }

    public static String getNbtItem(String string){
        return LittleMasterMainClass.getMasterMainClass().nbtItem.getString(string,"");
    }

    public static LinkedHashMap<String,Number> toRankList(LinkedHashMap<String, ? extends Number> map){
        LinkedHashMap<String,Number> rank = new LinkedHashMap<>();
        HashMap<String,Number> map1 = new LinkedHashMap<>();
        for(String n:map.keySet()){
            Number num = map.get(n);
            if(num instanceof Double){
                map1.put(n, new BigDecimal(num.toString()).setScale(2, RoundingMode.HALF_UP).doubleValue());
                continue;
            }
            map1.put(n, num);
        }
        List<Map.Entry<String, Number>> list = new ArrayList<>(map1.entrySet());
        list.sort((o1, o2) -> {
            if (Objects.equals(o1.getValue().doubleValue(), o2.getValue().doubleValue())) {
                return 0;
            }
            return o1.getValue().doubleValue() > o2.getValue().doubleValue() ? -1 : 1;
        });
        for(Map.Entry<String,Number> ma:list){
            rank.put(ma.getKey(),ma.getValue());
        }

        return rank;
    }

}
