package com.uninu.mod;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class KeyItem extends Item {
    public KeyItem(Properties p) { super(p); }

    public static String getCrate(ItemStack s) {
        return s.hasTag() ? s.getTag().getString("crate") : "";
    }

    @Override
    public boolean isFoil(ItemStack s) { return true; }
}
