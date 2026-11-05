package com.cobbleshop;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CobbleShop.MODID)
public class CobbleShop {
    public static final String MODID = "cobbleshop";
    public static final Logger LOGGER = LogUtils.getLogger();

    private int ticks = 0;

    public CobbleShop() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        ShopCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ShopStore.load();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ShopStore.saveShops();
        Economy.save();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++ticks >= 6000) { // toutes les 5 minutes
            ticks = 0;
            Economy.saveIfDirty();
        }
    }
}
