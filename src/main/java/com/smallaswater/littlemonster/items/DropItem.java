package com.smallaswater.littlemonster.items;

import cn.nukkit.item.Item;


/**
 * @author 若水
 */
public class DropItem extends BaseItem{

    private Item item;

    public DropItem(Item item, int round){
        super(round);
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    public static DropItem toItem(String s,int round){
        if(s.split(SPLIT_1).length > 1){
            if(NBT.equalsIgnoreCase(s.split(SPLIT_1)[1])){
                String id  = s.split(SPLIT_1)[0];
                int count = 1;
                if(id.split(SPLIT_2).length > 1){
                    count = Integer.parseInt(id.split(SPLIT_2)[1]);
                    id = id.split(SPLIT_2)[0];
                }
                Item item = toItem(id);
                if(item != null) {
                    item.setCount(count);
                    return new DropItem(item, round);
                }
            }else{
                return new DropItem(toStringItem(s.split(SPLIT_1)[0]),round);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return item.hasCompoundTag()?
                item.getId()+":"+item.getDamage()+":"+item.getCount()+"@nbt":
                item.getId()+":"+item.getDamage()+":"+item.getCount()+"@item";
    }
}
