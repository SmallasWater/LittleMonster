package com.smallaswater.littlemonster.items;

import cn.ankele.plugin.MagicItem;
import cn.ankele.plugin.bean.ItemBean;
import cn.ankele.plugin.utils.Tools;
import cn.nukkit.item.Item;
import com.smallaswater.littlemonster.LittleMonsterMainClass;

import java.util.LinkedHashMap;

import static cn.ankele.plugin.utils.BaseCommand.createItem;
import static com.smallaswater.littlemonster.events.LittleMasterListener.hasMagicItem;
import static java.lang.Integer.parseInt;


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
            } else if (MI.equalsIgnoreCase(s.split(SPLIT_1)[1])) {
                if (hasMagicItem) {
                    String original = s.split(SPLIT_1)[0];
                    LinkedHashMap<String, ItemBean> items = MagicItem.getItemsMap();
                    LinkedHashMap<String, Object> otherItems = MagicItem.getOthers();
                    String[] args = original.split(SPLIT_2);
                    if (items.containsKey(args[1])) {
                        ItemBean item = items.get(args[1]);
                        Item back = createItem(item);
                        back.setCount(parseInt(args[0]));
                        return new DropItem(back, round);
                    } else if (otherItems.containsKey(args[1])) {
                        String[] otherItemArr = ((String) otherItems.get(args[1])).split(":");
                        Item item = Item.get(parseInt(otherItemArr[0]), parseInt(otherItemArr[1]));
                        item.setCount(parseInt(args[0]));
                        item.setCompoundTag(Tools.hexStringToBytes(otherItemArr[3]));
                        return new DropItem(item, round);
                    } else {
                        LittleMonsterMainClass.getInstance().getLogger().warning("MagicItem物品不存在：" + args[1]);
                    }
                }
            } else{
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
