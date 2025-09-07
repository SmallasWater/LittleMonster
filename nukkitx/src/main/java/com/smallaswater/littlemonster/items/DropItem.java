package com.smallaswater.littlemonster.items;

import cn.nukkit.item.Item;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.utils.Utils;

import java.util.Map;

import static java.lang.Integer.parseInt;


/**
 * @author 若水
 */
public class DropItem extends BaseItem {

    private Item item;

    public DropItem(Item item, int round) {
        super(round);
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    public static DropItem toItem(String s, int round) {
        if (s.split(SPLIT_1).length > 1) {
            if (NBT.equalsIgnoreCase(s.split(SPLIT_1)[1])) {
                String id = s.split(SPLIT_1)[0];
                int count = 1;
                if (id.split(SPLIT_2).length > 1) {
                    count = Integer.parseInt(id.split(SPLIT_2)[1]);
                    id = id.split(SPLIT_2)[0];
                }
                Item item = toItem(id);
                if (item != null) {
                    item.setCount(count);
                    return new DropItem(item, round);
                }
            } else if (MI.equalsIgnoreCase(s.split(SPLIT_1)[1])) {
                if (LittleMonsterMainClass.hasMagicItem) {
                    try {
                        String original = s.split(SPLIT_1)[0];
                        Class<?> aClass = Class.forName("cn.ankele.plugin.MagicItem");
                        Map<String, Object> items = (Map<String, Object>) aClass.getMethod("getItemsMap").invoke(null);
                        Map<String, Object> otherItems = (Map<String, Object>) aClass.getMethod("getOthers").invoke(null);
                        String[] args = original.split(SPLIT_2);
                        if (items.containsKey(args[1])) {
                            Item back = (Item) Class.forName("cn.ankele.plugin.utils.BaseCommand")
                                    .getMethod("createItem", Class.forName("cn.ankele.plugin.bean.ItemBean"))
                                    .invoke(null, items.get(args[1]));
                            back.setCount(parseInt(args[0]));
                            return new DropItem(back, round);
                        } else if (otherItems.containsKey(args[1])) {
                            String[] otherItemArr = ((String) otherItems.get(args[1])).split(":");
                            Item item = Item.get(parseInt(otherItemArr[0]), parseInt(otherItemArr[1]));
                            item.setCount(parseInt(args[0]));
                            item.setCompoundTag(Utils.hexStringToBytes(otherItemArr[3]));
                            return new DropItem(item, round);
                        } else {
                            LittleMonsterMainClass.getInstance().getLogger().warning("MagicItem物品不存在：" + original);
                        }
                    } catch (Exception e) {
                        LittleMonsterMainClass.getInstance().getLogger().error("MagicItem物品解析错误：", e);
                    }
                } else {
                    LittleMonsterMainClass.getInstance().getLogger().warning("MagicItem 前置不存在，无法解析物品：" + s);
                }
            } else {
                return new DropItem(toStringItem(s.split(SPLIT_1)[0]), round);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        Object id = item.getId();
        if (stringItemClass != null && stringItemClass.isInstance(item) && getNamespaceIdMethod != null) {
            try {
                id = getNamespaceIdMethod.invoke(item);
                return item.hasCompoundTag() ?
                        id + ":" + item.getCount() + "@nbt" :
                        id + ":" + item.getCount() + "@item";
            } catch (Exception e) {
                LittleMonsterMainClass.getInstance().getLogger().error("获取物品命名空间ID失败：", e);
            }
        }
        return item.hasCompoundTag() ?
                id + ":" + item.getDamage() + ":" + item.getCount() + "@nbt" :
                id + ":" + item.getDamage() + ":" + item.getCount() + "@item";
    }
}
