package com.smallaswater.littlemonster.commands;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.config.MonsterConfig;
import com.smallaswater.littlemonster.config.PositionConfig;
import com.smallaswater.littlemonster.entity.IEntityCommandSender;
import com.smallaswater.littlemonster.items.BaseItem;
import com.smallaswater.littlemonster.utils.Utils;
import com.smallaswater.littlemonster.windows.LittleWindow;

import java.io.File;
import java.util.LinkedHashMap;

/**
 * @author 若水 &
 */
public class LittleNpcSpawnCommand extends Command {

    public LittleNpcSpawnCommand() {
        super("刷怪","生成命令","/lt help",new String[]{"lt"});

        this.commandParameters.clear();
        this.commandParameters.put("help", new CommandParameter[]{
                CommandParameter.newEnum("help", new String[]{"help"})
        });
        this.commandParameters.put("clear", new CommandParameter[]{
                CommandParameter.newEnum("clear", new String[]{"clear"})
        });
        this.commandParameters.put("create", new CommandParameter[]{
                CommandParameter.newEnum("create", new String[]{"create"}),
                CommandParameter.newType("name", CommandParamType.TEXT)
        });
        this.commandParameters.put("del", new CommandParameter[]{
                CommandParameter.newEnum("del", new String[]{"del"}),
                CommandParameter.newType("name", CommandParamType.TEXT)
        });
        this.commandParameters.put("pos", new CommandParameter[]{
                CommandParameter.newEnum("pos", new String[]{"pos"}),
                CommandParameter.newType("name", CommandParamType.TEXT),
                CommandParameter.newType("monster", CommandParamType.TEXT)
        });
        this.commandParameters.put("dp", new CommandParameter[]{
                CommandParameter.newEnum("dp", new String[]{"dp"}),
                CommandParameter.newType("name", CommandParamType.TEXT)
        });
        this.commandParameters.put("send", new CommandParameter[]{
                CommandParameter.newEnum("send", new String[]{"send"}),
                CommandParameter.newType("player", CommandParamType.TARGET),
                CommandParameter.newType("message", CommandParamType.TEXT)
        });
        this.commandParameters.put("spawn", new CommandParameter[]{
                CommandParameter.newEnum("spawn", new String[]{"spawn"}),
                CommandParameter.newType("name", CommandParamType.TEXT),
                CommandParameter.newType("x", CommandParamType.FLOAT),
                CommandParameter.newType("y", CommandParamType.FLOAT),
                CommandParameter.newType("z", CommandParamType.FLOAT),
                CommandParameter.newType("level", CommandParamType.TEXT),
                CommandParameter.newType("time", CommandParamType.TEXT)
        });
        this.commandParameters.put("set", new CommandParameter[]{
                CommandParameter.newEnum("set", new String[]{"set"})
        });
        this.commandParameters.put("reload", new CommandParameter[]{
                CommandParameter.newEnum("reload", new String[]{"reload"})
        });
        this.commandParameters.put("save", new CommandParameter[]{
                CommandParameter.newEnum("save", new String[]{"save"}),
                CommandParameter.newType("name", CommandParamType.TEXT)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] strings) {
        if(sender.isOp()){
            if(strings.length>=1) {
                if("help".equals(strings[0])){
                    sender.sendMessage("§e§l---------§a <§oLittleMonster Help§r§l§a>§e ---------");
                    sender.sendMessage("§b/lt §7clear §a清除所有实体");
                    sender.sendMessage("§b/lt §7create §6<名字>  §a创建怪物");
                    sender.sendMessage("§b/lt §7del §6<名字> §a删除怪物");
                    sender.sendMessage("§b/lt §7pos §6<名字> <怪物>  §a创建怪物点");
                    sender.sendMessage("§b/lt §7dp §6<名字> §a删除怪物点");
                    sender.sendMessage("§b/lt §7send §6<玩家> <消息> §a给玩家发送消息");
                    sender.sendMessage("§b/lt §7spawn §6<名字> <x> <y> <z> <level> <time(存活时间(秒))> §a生成一个怪物");
                    sender.sendMessage("§b/lt §7set §a修改怪物数据");
                    sender.sendMessage("§b/lt §7reload §a重新加载配置文件");
                    sender.sendMessage("§b/lt §7save §6<名字> §a将手持物品保存在 nbtitem.yml");
                    sender.sendMessage("§e§l---------§a <§oLittleMonster Help§r§l§a>§e ---------");
                }
                if("spawn".equalsIgnoreCase(strings[0])){
                    if(sender instanceof Player) {
                        if (strings.length > 1) {
                            if (LittleMonsterMainClass.getInstance().monsters.containsKey(strings[1])) {
                                int time = -1;
                                if(strings.length > 5){
                                    Position position = new Position(
                                            Double.parseDouble("~".equals(strings[2])?((Player) sender).getX()+"":strings[2]),
                                            Double.parseDouble("~".equals(strings[3])?((Player) sender).getY()+"":strings[3]),
                                            Double.parseDouble("~".equals(strings[4])?((Player) sender).getZ()+"":strings[4]),
                                            Server.getInstance().getLevelByName(strings[5]));
                                    if(strings.length > 6){
                                        time = Integer.parseInt(strings[6]);
                                    }
                                    LittleMonsterMainClass.getInstance().monsters.get(strings[1]).spawn(position,time);
                                }else{
                                    if(strings.length > 2){
                                        time = Integer.parseInt(strings[2]);
                                    }
                                    LittleMonsterMainClass.getInstance().monsters.get(strings[1]).spawn(((Player) sender).getPosition(),time);
                                }
                                sender.sendMessage("§a怪物 " + strings[1] + "生成"+((time > 0)?" 存活 §e"+time+"§a 秒":""));
                            } else {
                                sender.sendMessage("§c怪物 " + strings[1] + "不存在");
                            }
                        } else {
                            sender.sendMessage("§c指令格式错误!格式:/lt create <名字> <刷怪点类型>");
                            return false;
                        }
                    }else if(strings.length > 5){
                        if (LittleMonsterMainClass.getInstance().monsters.containsKey(strings[1])) {
                            Position position = new Position(Double.parseDouble(strings[2]),
                                    Double.parseDouble(strings[3]),
                                    Double.parseDouble(strings[4]),
                                    Server.getInstance().getLevelByName(strings[5]));
                            LittleMonsterMainClass.getInstance().monsters.get(strings[1]).spawn(position);
                            sender.sendMessage("§a怪物 " + strings[1] + "在"+position+"生成");
                        } else {
                            sender.sendMessage("§c怪物 " + strings[1] + "不存在");
                        }
                        return false;
                    }else{
                        if(strings.length > 2){
                            if (LittleMonsterMainClass.getInstance().monsters.containsKey(strings[1])) {
                                String name = strings[2];
                                Player p = Server.getInstance().getPlayer(name);
                                if(p != null){
                                    int time = -1;
                                    if(strings.length > 3){
                                        time = Integer.parseInt(strings[3]);
                                    }
                                    LittleMonsterMainClass.getInstance().monsters.get(strings[1]).spawn(p.getPosition(),time);
                                    sender.sendMessage("§a怪物 " + strings[1] + "在"+p.getPosition()+"生成");
                                }else{
                                    sender.sendMessage("§c玩家 " + name + "不在线");
                                }

                            } else {
                                sender.sendMessage("§c怪物 " + strings[1] + "不存在");
                            }

                            return true;
                        }
                        sender.sendMessage("请不要在控制台执行");
                    }
                }
                if("send".equalsIgnoreCase(strings[0])){
                    if(strings.length > 2){
                        Player player = Server.getInstance().getPlayer(strings[1]);
                        if(player != null){
                            StringBuilder stringBuilder = new StringBuilder();
                            for (int i = 2; i < strings.length; i++) {
                                stringBuilder.append(strings[i]).append("\n");
                            }
                            stringBuilder.deleteCharAt(stringBuilder.length()-1);
                            player.sendMessage(TextFormat.colorize('&', stringBuilder.toString()));
                           return true;
                        }else{
                            if(sender instanceof IEntityCommandSender){
                                return true;
                            }
                            sender.sendMessage("玩家不在线");
                            return false;
                        }
                    }else{
                        return false;
                    }
                }
                if("set".equalsIgnoreCase(strings[0])){
                    if(sender instanceof Player) {
                        LittleWindow.sendMenu((Player) sender);
                    }else{
                        sender.sendMessage("请不要在控制台执行");
                        return false;
                    }
                }
                if("clear".equals(strings[0])){
                    for(Level l: Server.getInstance().getLevels().values()){
                        for(Entity ent:l.getEntities()) {
                            if(Utils.isMonster(ent)){
                                ent.close();
                            }
                        }
                    }
                    sender.sendMessage("§a已清除所有副本生物");
                    return false;
                }
                if("pos".equals(strings[0])) {
                    if (sender instanceof Player) {
                        if (strings.length > 2) {
                            try {
                                String name = strings[1];
                                if (LittleMonsterMainClass.getInstance().positions.containsKey(name)) {
                                    sender.sendMessage("§c此刷怪点已经存在...");
                                    return false;
                                }
                                if (LittleMonsterMainClass.getInstance().monsters.containsKey(strings[2])) {
                                    Position pos = ((Player) sender).getPosition();
                                    LittleMonsterMainClass.getInstance().saveResource("position.yml", "/Position/" + name + ".yml", false);
                                    Config config = new Config(LittleMonsterMainClass.getInstance().getDataFolder() + "/Position/" + name + ".yml", Config.YAML);
                                    config.set("刷怪点", new LinkedHashMap<String, Object>() {
                                        {
                                            put("x", pos.x);
                                            put("y", pos.y);
                                            put("z", pos.z);
                                            put("level", pos.level.getFolderName());
                                        }
                                    });
                                    config.set("刷新怪物", strings[2]);
                                    config.save();
                                    PositionConfig easyEntity = PositionConfig.loadPosition(name, config);
                                    if (easyEntity != null) {
                                        LittleMonsterMainClass.getInstance().positions.put(name, easyEntity);
                                    } else {
                                        sender.sendMessage("§c创建失败...");
                                        return true;
                                    }
                                } else {
                                    sender.sendMessage("§c不存在" + strings[2] + "怪物");
                                    return true;
                                }
                                sender.sendMessage("§a创建成功");
                            } catch (Exception e) {
                                sender.sendMessage("§c创建刷怪点出现错误");
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }else{
                        sender.sendMessage("请不要在控制台执行");
                        return false;
                    }
                }
                if("create".equals(strings[0])){
                    if(sender instanceof Player) {
                        try {
                            String name = strings[1];
                            if (LittleMonsterMainClass.getInstance().monsters.containsKey(name)) {
                                sender.sendMessage("§c此怪物已经存在...");
                                return false;
                            }
                            LittleMonsterMainClass.getInstance().saveResource("monster.yml", "/Monster/" + name + ".yml", false);
                            Config config = new Config(LittleMonsterMainClass.getInstance().getDataFolder() + "/Monster/" + name + ".yml", Config.YAML);
                            config.save();
                            MonsterConfig easyEntity = MonsterConfig.loadEntity(name, config);
                            if (easyEntity != null) {
                                LittleMonsterMainClass.getInstance().monsters.put(name, easyEntity);
                            } else {
                                sender.sendMessage("§c创建失败...");
                                return true;
                            }

                            sender.sendMessage("§a创建成功");
                        } catch (Exception e) {
                            sender.sendMessage("§c创建怪物出现错误");
                            return false;
                        }
                    }else{
                        sender.sendMessage("请不要在控制台执行");
                        return false;
                    }
                }
                if("dp".equalsIgnoreCase(strings[0])){
                    if(strings.length > 1){
                        return delPos(strings[1], sender);
                    }
                }
                if("del".equals(strings[0])){
                    if(strings.length > 1){
                        String name = strings[1];
                        if(LittleMonsterMainClass.getInstance().monsters.containsKey(name)){
                            LittleMonsterMainClass.getInstance().monsters.remove(name);
                            if(!new File(LittleMonsterMainClass.getInstance().getDataFolder()+ "/Monster/"+name+".yml").delete()){
                                Server.getInstance().getLogger().error("删除 文件"+name+".yml 失败");
                            }
                            sender.sendMessage("§a怪物删除成功");
                            for(Level l: Server.getInstance().getLevels().values()){
                                for(Entity ent:l.getEntities()) {
                                    String m = Utils.getMonster(ent);
                                    if(m != null){
                                        if(m.equalsIgnoreCase(name)){
                                            ent.kill();
                                        }
                                    }
                                }
                            }
                        }else{
                            sender.sendMessage("§c刷怪点不存在");
                        }
                    }else{
                        return false;
                    }
                }
                if("reload".equalsIgnoreCase(strings[0])){
                    LittleMonsterMainClass.getInstance().init();
                    sender.sendMessage("§a配置文件重新读取完成");
                }
                if("save".equalsIgnoreCase(strings[0])){
                    if(sender instanceof Player) {
                        if (strings.length > 1) {
                            String name = strings[1];
                            if ("".equals(Utils.getNbtItem(name))) {
                                Item hand = ((Player) sender).getInventory().getItemInHand();
                                if (hand.getId() != 0) {
                                    String text = BaseItem.toStringItem(hand);
                                    Utils.saveNbt(name, text);
                                    sender.sendMessage("§a 成功将手持物品保存在 nbtitems.yml 名称:" + name);
                                } else {
                                    sender.sendMessage("§c请不要保存空气");
                                    return true;
                                }
                            } else {
                                sender.sendMessage("§c" + name + "已存在..");
                            }
                        } else {
                            return false;
                        }
                    }else{
                        sender.sendMessage("请不要在控制台执行");
                        return false;
                    }
                }
            }else{
                sender.sendMessage("§c指令格式错误!格式:/lt create <名字> <刷怪点类型>");
                return false;
            }
        }
        return false;
    }

    private boolean delPos(String name,CommandSender sender){
        if(LittleMonsterMainClass.getInstance().positions.containsKey(name)){
            LittleMonsterMainClass.getInstance().positions.remove(name);
            if(!new File(LittleMonsterMainClass.getInstance().getDataFolder()+ "/Position/"+name+".yml").delete()){
                Server.getInstance().getLogger().error("删除 文件"+name+".yml 失败");
            }
            sender.sendMessage("§a刷怪点删除成功");
            return true;
        }else{
            sender.sendMessage("§c刷怪点不存在");
        }
        return false;
    }
}
