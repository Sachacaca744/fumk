package com.cobbleshop;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class Util {
    private Util() {}

    /** 1234567 -> "$1 234 567" */
    public static String money(long v) {
        String s = Long.toString(v);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) b.append(' ');
            b.append(s.charAt(i));
        }
        return ShopStore.config.symbol + b;
    }

    /** Texte sans italique (pour noms et descriptions d'objets). */
    public static Component line(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(Style.EMPTY.withItalic(false).withColor(color));
    }

    public static Component msg(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    public static void say(ServerPlayer p, String text, ChatFormatting color) {
        p.sendSystemMessage(msg(text, color));
    }

    public static ItemStack named(ItemStack st, String name, ChatFormatting color) {
        st.setHoverName(line(name, color));
        return st;
    }

    public static void addLore(ItemStack st, List<Component> lines) {
        CompoundTag display = st.getOrCreateTagElement("display");
        ListTag lore = display.contains("Lore", Tag.TAG_LIST) ? display.getList("Lore", Tag.TAG_STRING) : new ListTag();
        for (Component c : lines) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        display.put("Lore", lore);
    }
}
