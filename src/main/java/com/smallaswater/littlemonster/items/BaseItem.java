package com.smallaswater.littlemonster.items;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import com.smallaswater.littlemonster.utils.Utils;

public abstract class BaseItem {

    protected static final String SPLIT_1 = "@";

    protected static final String SPLIT_3 = "&";

    protected static final String SPLIT_2 = ":";

    protected static final String NBT = "nbt";
    protected static final String MI = "mi";

    protected static final String NOT = "not";

    public static final String TARGET = "target";

    public static final String TARGETALL = "targetAll";

    public static final String DAMAGE = "damage";

    /**
     * 触发概率
     */
    private int round;

    public BaseItem(int round) {
        this.round = round;
    }

    public int getRound() {
        return round;
    }

    //id:damage:count:nbt
    static Item toItem(String str) {
        String configString = Utils.getNbtItem(str);
        if (!"".equals(configString)) {
            String[] strings = configString.split(SPLIT_2);
            Item item = Item.get(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]), Integer.parseInt(strings[2]));
            if (!NOT.equals(strings[3])) {
                byte[] bytes = Utils.hexStringToBytes(strings[3]);
                if (bytes != null) {
                    CompoundTag tag = Item.parseCompoundTag(bytes);
                    item.setNamedTag(tag);
                }
            }
            return item;
        }
        return null;
    }

    @Deprecated
    public static String bytesToHexString(byte[] src) {
        return Utils.bytesToHexString(src);
    }

    static Item toStringItem(String i) {
        String[] items = i.split(SPLIT_2);
        if (items.length > 1) {
            Item item = Item.get(Integer.parseInt(items[0]), Integer.parseInt(items[1]));
            if (items.length > 2) {
                item.setCount(Integer.parseInt(items[2]));
            }
            return item;
        }
        return Item.get(0);
    }

    public static String toStringItem(Item item) {
        String tag = "not";
        if (item.hasCompoundTag()) {
            tag = Utils.bytesToHexString(item.getCompoundTag());
        }
        return item.getId() + ":" + item.getDamage() + ":" + item.getCount() + ":" + tag;
    }

}
