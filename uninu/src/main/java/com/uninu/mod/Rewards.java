package com.uninu.mod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

public class Rewards {
    private static final Random RNG = new Random();

    public static CrateStore.Reward pick(CrateStore.Crate c) {
        int total = 0;
        for (CrateStore.Reward r : c.rewards) total += Math.max(1, r.weight);
        if (total <= 0) return null;
        int roll = RNG.nextInt(total);
        for (CrateStore.Reward r : c.rewards) {
            roll -= Math.max(1, r.weight);
            if (roll < 0) return r;
        }
        return c.rewards.get(0);
    }

    public static ItemStack parseItem(String data) {
        try {
            String d = data.trim();
            if (d.startsWith("{")) return ItemStack.of(TagParser.parseTag(d));
            String[] parts = d.split("\\s+");
            Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(parts[0]));
            if (it == null || it == Items.AIR) return ItemStack.EMPTY;
            int n = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            return new ItemStack(it, Math.max(1, Math.min(n, 64)));
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public static void giveStack(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    /** Donne la récompense. Renvoie false si elle est invalide. */
    public static boolean give(ServerPlayer p, CrateStore.Reward r) {
        if ("COMMAND".equals(r.type)) {
            String cmd = r.data.replace("%player%", p.getGameProfile().getName());
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            p.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4), cmd);
            return true;
        }
        ItemStack s = parseItem(r.data);
        if (s.isEmpty()) return false;
        giveStack(p, s);
        return true;
    }

    public static ItemStack makeKey(CrateStore.Crate c) {
        ItemStack key = new ItemStack(ModRegistry.KEY.get());
        key.getOrCreateTag().putString("crate", c.id);
        key.setHoverName(Component.literal("§eClef : " + c.name));
        return key;
    }

    public static ItemStack makeChest(CrateStore.Crate c) {
        ItemStack chest = new ItemStack(ModRegistry.CRATE_ITEM.get());
        CompoundTag be = new CompoundTag();
        be.putString("crate", c.id);
        chest.getOrCreateTag().put("BlockEntityTag", be);
        chest.setHoverName(Component.literal("§6Coffre : " + c.name));
        return chest;
    }
}
