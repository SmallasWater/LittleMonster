package com.smallaswater.littlemonster.flot;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import cn.nukkit.network.protocol.SetEntityDataPacket;
import com.smallaswater.littlemonster.manager.PlayerFlotTextManager;
import com.smallaswater.littlemonster.utils.Utils;

public class FlotText extends FloatingTextParticle {


    private Position position;

    private String name;

    private Player player;

    public FlotText(String name, Position pos, String title, Player player) {
        super(Location.fromObject(pos,pos.getLevel()), title);
        this.player = player;
        this.position = pos.add(-0.5,-2,-0.5);
        this.name = name;
    }

    public Position getPosition() {
        return position;
    }

    public Player getPlayer() {
        return player;
    }

    public void toUpData(){
        if(level != null) {
            SetEntityDataPacket packet = new SetEntityDataPacket();
            packet.eid = this.entityId;
            packet.metadata = this.metadata;
            Server.getInstance().getOnlinePlayers().values().forEach(player -> player.dataPacket(packet));
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void kill() {
        RemoveEntityPacket pk = new RemoveEntityPacket();
        pk.eid = getEntityId();
        Server.getInstance().getOnlinePlayers().values().forEach(player -> player.dataPacket(pk));
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof FlotText){
            if (((FlotText) obj).getName().equalsIgnoreCase(getName())) {
                Utils.positionEqual(((FlotText) obj).position, position);
            }
        }
        return false;
    }
}
