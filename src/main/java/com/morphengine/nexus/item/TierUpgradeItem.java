package com.morphengine.nexus.item;

import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.TieredBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Consumer;

/**
 * A tier upgrade: a right click while sneaking on a block of the tier below turns it into one of this tier, and it
 * keeps all that it held, its energy, its upgrades, its name and its settings. A tier cannot be skipped: it takes the
 * upgrade of every tier on the way, in order.
 */
public final class TierUpgradeItem extends Item {

    private static final int PARTICLES = 12;
    private static final double PARTICLE_SPREAD = 0.4;
    private static final double PARTICLE_SPEED = 0.02;
    private static final double PARTICLE_HEIGHT = 0.6;
    private static final double BLOCK_MIDDLE = 0.5;
    private static final float SOUND_VOLUME = 0.6F;
    private static final float SOUND_PITCH = 1.4F;

    private final int targetRank;

    /**
     * @param targetRank the rank of the tier the upgrade makes, from 2: the first tier is what a block starts as
     */
    public TierUpgradeItem(final int targetRank, final Item.Properties properties) {
        super(properties);
        if (targetRank < 2) {
            throw new IllegalArgumentException("a tier upgrade makes the second tier or a higher one: " + targetRank);
        }
        this.targetRank = targetRank;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        final Level level = context.getLevel();
        final BlockState state = level.getBlockState(context.getClickedPos());
        if (player == null || !player.isSecondaryUseActive() || !(state.getBlock() instanceof TieredBlock tiered)) {
            return InteractionResult.PASS;
        }
        return tiered.rank() == targetRank - 1 ? raise(context, player, tiered) : refuse(level, player, tiered);
    }

    private InteractionResult raise(final UseOnContext context, final Player player, final TieredBlock tiered) {
        final Block next = tiered.nextTier();
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (next == null) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!NetworkAccess.permitsOrOwns(player, level.getBlockEntity(pos), Permission.CONFIGURE)) {
            NetworkAccess.refuse(player, Permission.CONFIGURE);
            return InteractionResult.FAIL;
        }
        level.setBlock(pos, next.withPropertiesOf(level.getBlockState(pos)), Block.UPDATE_ALL);
        context.getItemInHand().consume(1, player);
        celebrate((ServerLevel) level, pos);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult refuse(final Level level, final Player player, final TieredBlock tiered) {
        if (!level.isClientSide()) {
            final String reason = tiered.rank() >= targetRank ? "already" : "skips";
            player.displayClientMessage(Component.translatable("message.nexus.tier_upgrade." + reason)
                    .withStyle(ChatFormatting.RED), true);
        }
        return InteractionResult.FAIL;
    }

    private static void celebrate(final ServerLevel level, final BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, SOUND_VOLUME, SOUND_PITCH);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + BLOCK_MIDDLE, pos.getY() + PARTICLE_HEIGHT,
                pos.getZ() + BLOCK_MIDDLE, PARTICLES, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD,
                PARTICLE_SPEED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            final ItemStack stack, final Item.TooltipContext context, final List<Component> tooltip,
            final TooltipFlag flag) {
        final Consumer<Component> builder = tooltip::add;
        builder.accept(Component.translatable("tooltip.nexus.tier_upgrade.use").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.nexus.tier_upgrade.keeps").withStyle(ChatFormatting.GRAY));
    }
}
