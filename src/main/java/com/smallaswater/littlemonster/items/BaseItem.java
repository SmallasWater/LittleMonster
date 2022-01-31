package com.smallaswater.littlemonster.items;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import com.smallaswater.littlemonster.utils.Utils;


public abstract class BaseItem {

    private int round;

    protected static final String SPLIT_1 = "@";

    protected static final String SPLIT_3 = "&";

    protected static final String SPLIT_2 = ":";

    protected static final String NBT = "nbt";

    protected static final String NOT = "not";

    public static final String TARGET = "target";

    public static final String TARGETALL = "targetAll";

    public static final String DAMAGE = "damage";

    public BaseItem(int round){
        this.round = round;
    }

    public int getRound() {
        return round;
    }

    //id:damage:count:nbt
    static Item toItem(String str){
        String configString = Utils.getNbtItem(str);
        if(!"".equals(configString)){
            String[] strings = configString.split(SPLIT_2);
            Item item = new Item(Integer.parseInt(strings[0]),Integer.parseInt(strings[1]));
            item.setCount(Integer.parseInt(strings[2]));
            if(!NOT.equals(strings[3])){
                byte[] bytes = hexStringToBytes(strings[3]);
                if(bytes != null){
                    CompoundTag tag = Item.parseCompoundTag(bytes);
                    item.setNamedTag(tag);
                }
            }
            return item;
        }
        return null;
    }

    private static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || "".equals(hexString)) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    static Item toStringItem(String i){
        String[] items = i.split(SPLIT_2);
        if(items.length > 1){
            if(items.length > 2){
                Item item = Item.get(Integer.parseInt(items[0]),Integer.parseInt(items[1]));
                item.setCount(Integer.parseInt(items[2]));
                return item;
            }
            return Item.get(Integer.parseInt(items[0]),Integer.parseInt(items[1]));
        }
        return new Item(0,0);
    }

    public static String toStringItem(Item item){
        String tag = "not";
        if(item.hasCompoundTag()){
            tag = bytesToHexString(item.getCompoundTag());
        }
        return item.getId()+":"+item.getDamage()+":"+item.getCount()+":"+tag;
    }

}
