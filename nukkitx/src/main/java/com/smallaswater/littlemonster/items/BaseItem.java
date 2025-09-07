package com.smallaswater.littlemonster.items;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import com.smallaswater.littlemonster.LittleMonsterMainClass;
import com.smallaswater.littlemonster.utils.Utils;

import java.lang.reflect.Method;

public abstract class BaseItem {

    protected static Class<?> stringItemClass;
    protected static Method getNamespaceIdMethod;

    static {
        try {
            stringItemClass = Class.forName("cn.nukkit.item.StringItem");
            getNamespaceIdMethod = stringItemClass.getMethod("getNamespaceId");
            getNamespaceIdMethod.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {

        }
    }

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
            Item item = Item.fromString(strings[0] + ":" + strings[1]);
            item.setCount(Integer.parseInt(strings[2]));
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
            Item item = Item.fromString(items[0] + ":" + items[1]);
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
        if (stringItemClass != null && stringItemClass.isInstance(item) && getNamespaceIdMethod != null) {
            try {
                Object invoke = getNamespaceIdMethod.invoke(item);
                return invoke + ":" + item.getCount() + ":" + tag;
            } catch (Exception e) {
                LittleMonsterMainClass.getInstance().getLogger().error("获取物品命名空间ID失败：", e);
            }
        }
        return item.getId() + ":" + item.getDamage() + ":" + item.getCount() + ":" + tag;
    }

}
