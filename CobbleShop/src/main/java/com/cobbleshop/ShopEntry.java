package com.cobbleshop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

/** Un article : soit un objet reel, soit une commande (l'objet sert alors d'icone). */
public class ShopEntry {
    public String item;      // ItemStack au format SNBT
    public long buy;         // prix d'achat (0 = non achetable)
    public long sell;        // prix de rachat (0 = non vendable)
    public String command;   // si non vide : commande executee a l'achat ({player} = nom du joueur)

    public ShopEntry() {}

    public ShopEntry(ItemStack stack, long buy, long sell, String command) {
        this.item = encode(stack);
        this.buy = buy;
        this.sell = sell;
        this.command = command;
    }

    public static String encode(ItemStack s) {
        return s.save(new CompoundTag()).toString();
    }

    public static ItemStack decode(String snbt) {
        try {
            return ItemStack.of(TagParser.parseTag(snbt));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    /** Renvoie une copie neuve de l'objet (ou ItemStack.EMPTY si introuvable). */
    public ItemStack stack() {
        return decode(item);
    }

    public boolean isCommand() {
        return command != null && !command.isBlank();
    }
}
