package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.skill.BaseSkillAreaManager;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import lombok.Setter;

import java.util.ArrayList;

/**
 * @author LT_Name
 */
@Setter
public class CommandHealthSkill extends BaseSkillManager implements BaseSkillAreaManager {

    private ArrayList<String> commands = new ArrayList<>();

    public CommandHealthSkill(String name) {
        super(name);
        this.mode = 1;
    }

    @Override
    protected void privateDisplay(Entity... entities) {
        for (String cmd : this.commands) {
            if (cmd.contains("@p")) {
                for (Entity player : entities) {
                    if (player instanceof Player) {
                        cmd = cmd.replace("@p", player.getName());
                        executeCommand(cmd, (Player) player);
                    }
                }
            } else {
                executeCommand(cmd, null);
            }
        }
    }

    protected void executeCommand(String command, Player player) {
        String[] c = command.split("&");
        String cmd = c[0];
        if (cmd.startsWith("/")) {
            cmd = cmd.replaceFirst("/", "");
        }
        if (c.length > 1) {
            if ("con".equals(c[1])) {
                try {
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), cmd);
                } catch (Exception e) {
                    LittleMonsterMainClass.getInstance().getLogger().error("控制台权限执行命令时出现错误！", e);
                }
                return;
            } else if ("op".equals(c[1])) {
                if (player == null) {
                    throw new RuntimeException("OP权限执行命令时，玩家不能为空！");
                }
                boolean needCancelOP = false;
                if (!player.isOp()) {
                    needCancelOP = true;
                    player.setOp(true);
                }
                try {
                    Server.getInstance().dispatchCommand(player, cmd);
                } catch (Exception e) {
                    LittleMonsterMainClass.getInstance().getLogger().error("OP权限执行命令时出现错误！", e);
                } finally {
                    if (needCancelOP) {
                        player.setOp(false);
                    }
                }
                return;
            }
        }
        try {
            if (player == null) {
                throw new RuntimeException("玩家权限执行命令时，玩家不能为空！");
            }
            Server.getInstance().dispatchCommand(player, cmd);
        } catch (Exception e) {
            LittleMonsterMainClass.getInstance().getLogger().error("玩家权限执行命令时出现错误！", e);
        }
    }
}
