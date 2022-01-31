package com.smallaswater.littlemonster.manager;

import cn.nukkit.Player;
import cn.nukkit.level.Position;
import com.smallaswater.littlemonster.LittleMasterMainClass;
import com.smallaswater.littlemonster.flot.FlotText;
import com.smallaswater.littlemonster.utils.Utils;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author SmallasWater
 * Create on 2021/6/29 8:34
 * Package com.smallaswater.littlemonster.manager
 */
public class PlayerFlotTextManager {

    private Player player;

    private ArrayList<FlotText> flotTexts = new ArrayList<>();

    private PlayerFlotTextManager(Player player){
        this.player = player;
    }

    public void add(FlotText text){
        flotTexts.add(text);
    }

    public FlotText get(Position text){
        for(FlotText t:flotTexts){
            if(Utils.positionEqual(t.getPosition(),text)){
                return t;
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

    public void remove(FlotText text){
        flotTexts.remove(text);

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

    public static PlayerFlotTextManager getInstance(Player player){

        PlayerFlotTextManager flotTextManager = new PlayerFlotTextManager(player);
        if(!LittleMasterMainClass.getMasterMainClass().texts.contains(flotTextManager)){
            LittleMasterMainClass.getMasterMainClass().texts.add(flotTextManager);
        }
        return LittleMasterMainClass.getMasterMainClass().texts.get(LittleMasterMainClass.getMasterMainClass().texts.indexOf(flotTextManager));
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
