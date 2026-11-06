package com.uninu.mod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CrateBlock extends BaseEntityBlock {
    public CrateBlock(Properties p) { super(p); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrateBE(pos, state); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    private static boolean matches(ItemStack s, String id) {
        return s.getItem() instanceof KeyItem && id.equals(KeyItem.getCrate(s));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof CrateBE be)) return InteractionResult.PASS;

        ItemStack key = matches(sp.getMainHandItem(), be.crateId) ? sp.getMainHandItem()
                : matches(sp.getOffhandItem(), be.crateId) ? sp.getOffhandItem() : null;
        if (key == null) {
            sp.displayClientMessage(Component.literal("§cIl te faut la clef de ce coffre (en main)."), true);
            return InteractionResult.CONSUME;
        }
        CrateStore.Crate crate = CrateStore.find(be.crateId);
        if (crate == null || crate.rewards.isEmpty()) {
            sp.displayClientMessage(Component.literal("§cCe coffre n'a aucune récompense configurée."), true);
            return InteractionResult.CONSUME;
        }
        CrateStore.Reward r = Rewards.pick(crate);
        if (r == null || !Rewards.give(sp, r)) {
            sp.displayClientMessage(Component.literal("§cRécompense invalide, la clef n'est pas consommée."), true);
            return InteractionResult.CONSUME;
        }
        key.shrink(1); // la clef disparaît
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1f, 1f);
        sp.sendSystemMessage(Component.literal("§6[" + crate.name + "] §fTu as obtenu : §e" + r.name));
        return InteractionResult.CONSUME;
    }
}
