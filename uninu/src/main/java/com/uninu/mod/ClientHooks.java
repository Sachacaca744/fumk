package com.uninu.mod;

import net.minecraft.client.Minecraft;

import java.util.List;

public class ClientHooks {
    public static void open(List<CrateStore.Crate> crates) {
        Minecraft.getInstance().setScreen(new CrateListScreen(crates));
    }
}
