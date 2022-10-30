package com.smallaswater.littlemonster.manager;

import cn.nukkit.Player;
import cn.nukkit.level.Position;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.flot.FlotText;
import com.smallaswater.littlemonster.utils.Utils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author SmallasWater
 * Create on 2021/6/29 8:34
 * Package com.smallaswater.littlemonster.manager
 */
public class PlayerFlotTextManager {

    @Getter
    private final Player player;

    private final ArrayList<FlotText> flotTexts = new ArrayList<>();

    private PlayerFlotTextManager(Player player){
        this.player = player;
    }

    public void add(FlotText text){
        flotTexts.add(text);
    }

    public FlotText get(Position text){
        for (FlotText flotText : this.flotTexts) {
            if (Utils.positionEqual(flotText.getPosition(), text)) {
                return flotText;
            }
        }
        return null;
    }

    public ArrayList<FlotText> getFlotTexts() {
        return flotTexts;
    }

    public int size(){
        return flotTexts.size();
    }

    public void remove(FlotText text) {
        this.flotTexts.remove(text);
    }

    public void remove(Position text){
        Iterator<FlotText> flotTextIterator = flotTexts.iterator();
        FlotText f;
        while (flotTextIterator.hasNext()){
            f = flotTextIterator.next();
            if(Utils.positionEqual(f.getPosition(),text)){
                flotTextIterator.remove();
            }
        }
    }

    public boolean hasPosition(Position position){
        for(FlotText t:flotTexts){
            if(Utils.positionEqual(t.getPosition(),position)){
                return true;
            }
        }
        return false;
    }

    public static PlayerFlotTextManager getOrCreate(Player player) {
        PlayerFlotTextManager flotTextManager = new PlayerFlotTextManager(player);
        if(!LittleMonsterMainClass.getMasterMainClass().playerFlotTextManagers.contains(flotTextManager)) {
            LittleMonsterMainClass.getMasterMainClass().playerFlotTextManagers.add(flotTextManager);
        }
        return LittleMonsterMainClass.getMasterMainClass().playerFlotTextManagers.get(LittleMonsterMainClass.getMasterMainClass().playerFlotTextManagers.indexOf(flotTextManager));
    }

    public static PlayerFlotTextManager get(Player player) {
        for (PlayerFlotTextManager flotTextManager : LittleMonsterMainClass.getMasterMainClass().playerFlotTextManagers) {
            if (flotTextManager.player.equals(player)) {
                return flotTextManager;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Player: "+player.getName()+"text:["+flotTexts+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof PlayerFlotTextManager){
            return ((PlayerFlotTextManager) obj).player.equals(player);
        }
        return false;
    }
}
