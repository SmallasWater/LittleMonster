package com.smallaswater.littlemonster.entity;

import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.lang.TextContainer;
import cn.nukkit.permission.PermissibleBase;
import cn.nukkit.permission.Permission;
import cn.nukkit.permission.PermissionAttachment;
import cn.nukkit.permission.PermissionAttachmentInfo;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.utils.MainLogger;

import java.util.Map;

/**
 * @author SmallasWater
 * Create on 2021/9/9 17:41
 * Package com.smallaswater.littlemonster.entity
 */
public interface IEntityCommandSender extends CommandSender {

    PermissibleBase perm = null;

    String name = "";

    @Override
    boolean isPermissionSet(String name);

    @Override
    default boolean isPermissionSet(Permission permission) {
        return this.perm.isPermissionSet(permission);
    }

    @Override
    default boolean hasPermission(String name) {
        return this.perm.hasPermission(name);
    }

    @Override
    default boolean hasPermission(Permission permission) {
        return this.perm.hasPermission(permission);
    }

    @Override
    default PermissionAttachment addAttachment(Plugin plugin) {
        return this.perm.addAttachment(plugin);
    }

    @Override
    default PermissionAttachment addAttachment(Plugin plugin, String name) {
        return this.perm.addAttachment(plugin, name);
    }

    @Override
    default PermissionAttachment addAttachment(Plugin plugin, String name, Boolean value) {
        return this.perm.addAttachment(plugin, name, value);
    }

    @Override
    default void removeAttachment(PermissionAttachment attachment) {
        this.perm.removeAttachment(attachment);
    }

    @Override
    default void recalculatePermissions() {
        this.perm.recalculatePermissions();
    }

    @Override
    default Map<String, PermissionAttachmentInfo> getEffectivePermissions() {
        return this.perm.getEffectivePermissions();
    }

    @Override
    default boolean isPlayer() {
        return false;
    }

    @Override
    default Server getServer() {
        return Server.getInstance();
    }

    @Override
    default void sendMessage(String message) {
        message = this.getServer().getLanguage().translateString(message);
        String[] var2 = message.trim().split("\n");
        for (String line : var2) {
            MainLogger.getLogger().info(getName()+">> "+line);
        }

    }

    @Override
    default void sendMessage(TextContainer message) {
        this.sendMessage(this.getServer().getLanguage().translate(message));
    }

    @Override
    default String getName() {
        return name;
    }

    @Override
    default boolean isOp() {
        return true;
    }

    @Override
    default void setOp(boolean value) {
    }
}
