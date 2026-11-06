package com.uninu.mod;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;

@Mod(UninuMod.MODID)
public class UninuMod {
    public static final String MODID = "uninu";

    public UninuMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.BLOCKS.register(bus);
        ModRegistry.ITEMS.register(bus);
        ModRegistry.BES.register(bus);
        Net.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    /** /uninu : réservé aux OP (niveau 2) */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("uninu")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Net.OpenGui(CrateStore.all()));
                    return 1;
                }));
    }
}
