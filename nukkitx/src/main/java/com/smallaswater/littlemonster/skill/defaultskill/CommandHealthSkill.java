package com.smallaswater.littlemonster.skill.defaultskill;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.skill.BaseSkillManager;
import lombok.Setter;

import java.util.ArrayList;

/**
 * 命令技能：不依赖 boss 战斗目标，在血量触发时执行任意服务器命令。
 *
 * 注意：有意不实现 {@code BaseSkillAreaManager}。该接口是「范围技能」标记接口，
 * 会让分发逻辑落入依赖 {@code getFollowTarget()} 的分支，导致 boss 没有攻击目标时
 * 命令被静默跳过。命令技能语义上与目标无关，因此走 {@code onHealthListener} 的
 * else 分支单独处理。
 *
 * @author LT_Name
 */
@Setter
public class CommandHealthSkill extends BaseSkillManager {

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
                LittleMonsterMainClass.getInstance().getLogger().warning("命令技能执行时玩家为空，改用控制台权限执行: " + cmd);
                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), cmd);
            } else {
                Server.getInstance().dispatchCommand(player, cmd);
            }
        } catch (Exception e) {
            LittleMonsterMainClass.getInstance().getLogger().error("玩家权限执行命令时出现错误！", e);
        }
    }
}
