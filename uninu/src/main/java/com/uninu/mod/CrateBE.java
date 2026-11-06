package com.uninu.mod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrateBE extends BlockEntity {
    public String crateId = "";

    public CrateBE(BlockPos pos, BlockState state) {
        super(ModRegistry.CRATE_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("crate", crateId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        crateId = tag.getString("crate");
    }
}
