package com.smallaswater.littlemonster.flot;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import cn.nukkit.network.protocol.SetEntityDataPacket;
import com.smallaswater.littlemonster.utils.Utils;

public class FlotText extends FloatingTextParticle {

    private final Position position;

    private final String name;

    private final Player player;

    public FlotText(String name, Position pos, String title, Player player) {
        super(Location.fromObject(pos, pos.getLevel()), title);
        this.player = player;
        this.position = pos.add(-0.5, -2, -0.5);
        this.name = name;
    }

    public Position getPosition() {
        return position;
    }

    public Player getPlayer() {
        return player;
    }

    public void toUpData() {
        if (level != null) {
            SetEntityDataPacket packet = new SetEntityDataPacket();
            packet.eid = this.entityId;
            packet.metadata = this.metadata;
            Server.broadcastPacket(this.level.getPlayers().values(), packet);
            //Server.getInstance().getOnlinePlayers().values().forEach(player -> player.dataPacket(packet));
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void close() {
        RemoveEntityPacket pk = new RemoveEntityPacket();
        pk.eid = this.getEntityId();
        Server.broadcastPacket(Server.getInstance().getOnlinePlayers().values(), pk);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof FlotText) {
            if (((FlotText) obj).getName().equalsIgnoreCase(getName())) {
                return Utils.positionEqual(((FlotText) obj).position, position);
            }
        }
        return false;
    }
}
