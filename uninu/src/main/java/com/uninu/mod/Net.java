package com.uninu.mod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Net {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UninuMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, OpenGui.class, OpenGui::encode, OpenGui::decode, OpenGui::handle);
        CHANNEL.registerMessage(1, Save.class, Save::encode, Save::decode, Save::handle);
        CHANNEL.registerMessage(2, Final.class, Final::encode, Final::decode, Final::handle);
    }

    private static void write(FriendlyByteBuf b, List<CrateStore.Crate> list) {
        b.writeVarInt(list.size());
        for (CrateStore.Crate c : list) {
            b.writeUtf(c.id, 64);
            b.writeUtf(c.name, 64);
            b.writeVarInt(c.rewards.size());
            for (CrateStore.Reward r : c.rewards) {
                b.writeUtf(r.name, 64);
                b.writeUtf(r.type, 16);
                b.writeUtf(r.data, 4096);
                b.writeVarInt(r.weight);
            }
        }
    }

    private static List<CrateStore.Crate> read(FriendlyByteBuf b) {
        List<CrateStore.Crate> list = new ArrayList<>();
        int n = Math.min(b.readVarInt(), 50);
        for (int i = 0; i < n; i++) {
            CrateStore.Crate c = new CrateStore.Crate(b.readUtf(64), b.readUtf(64));
            int m = Math.min(b.readVarInt(), 200);
            for (int j = 0; j < m; j++) {
                c.rewards.add(new CrateStore.Reward(b.readUtf(64), b.readUtf(16), b.readUtf(4096), b.readVarInt()));
            }
            list.add(c);
        }
        return list;
    }

    /** Serveur -> client : ouvre l'interface */
    public static class OpenGui {
        final List<CrateStore.Crate> crates;
        public OpenGui(List<CrateStore.Crate> c) { crates = c; }
        static void encode(OpenGui m, FriendlyByteBuf b) { write(b, m.crates); }
        static OpenGui decode(FriendlyByteBuf b) { return new OpenGui(read(b)); }
        static void handle(OpenGui m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.open(m.crates)));
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> serveur : sauvegarde */
    public static class Save {
        final List<CrateStore.Crate> crates;
        public Save(List<CrateStore.Crate> c) { crates = c; }
        static void encode(Save m, FriendlyByteBuf b) { write(b, m.crates); }
        static Save decode(FriendlyByteBuf b) { return new Save(read(b)); }
        static void handle(Save m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p == null || !p.hasPermissions(2)) return;
                List<CrateStore.Crate> clean = CrateStore.sanitize(m.crates);
                CrateStore.set(clean);
                p.displayClientMessage(Component.literal("§a[UNINU] " + clean.size() + " coffre(s) sauvegardé(s)."), false);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Client -> serveur : sauvegarde + donne 1 clef et 1 coffre */
    public static class Final {
        final List<CrateStore.Crate> crates;
        final String crateId;
        public Final(List<CrateStore.Crate> c, String id) { crates = c; crateId = id; }
        static void encode(Final m, FriendlyByteBuf b) { write(b, m.crates); b.writeUtf(m.crateId, 64); }
        static Final decode(FriendlyByteBuf b) { return new Final(read(b), b.readUtf(64)); }
        static void handle(Final m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p == null || !p.hasPermissions(2)) return;
                CrateStore.set(CrateStore.sanitize(m.crates));
                CrateStore.Crate c = CrateStore.find(m.crateId);
                if (c == null) {
                    p.displayClientMessage(Component.literal("§c[UNINU] Coffre introuvable."), false);
                    return;
                }
                Rewards.giveStack(p, Rewards.makeKey(c));
                Rewards.giveStack(p, Rewards.makeChest(c));
                p.displayClientMessage(Component.literal("§a[UNINU] Config sauvegardée. Tu as reçu 1 clef + 1 coffre : " + c.name), false);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
