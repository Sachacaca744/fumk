package com.uninu.mod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, UninuMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, UninuMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, UninuMod.MODID);

    public static final RegistryObject<Block> CRATE = BLOCKS.register("crate_chest",
            () -> new CrateBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F)
                    .sound(SoundType.AMETHYST_BLOCK).noLootTable()));

    public static final RegistryObject<Item> CRATE_ITEM = ITEMS.register("crate_chest",
            () -> new BlockItem(CRATE.get(), new Item.Properties()));

    public static final RegistryObject<Item> KEY = ITEMS.register("crate_key",
            () -> new KeyItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<BlockEntityType<CrateBE>> CRATE_BE = BES.register("crate_chest",
            () -> BlockEntityType.Builder.of(CrateBE::new, CRATE.get()).build(null));
}
