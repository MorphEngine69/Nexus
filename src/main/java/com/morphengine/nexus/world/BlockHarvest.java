package com.morphengine.nexus.world;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.resource.ItemKey;
import com.morphengine.nexus.storage.ResourceCounter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;

/**
 * One block broken by a Remover: filtered by what it is as an item, broken
 * with the Remover's tool only when the network takes everything it drops,
 * and only when nothing that protects the block objects. What the network
 * turns away after all, as it may when the drops share its last room, drops
 * where the block stood. Server thread only.
 */
final class BlockHarvest {

    private final FrontSpace space;
    private final HarvestTool tool;

    BlockHarvest(final FrontSpace space, final HarvestTool tool) {
        this.space = space;
        this.tool = tool;
    }

    /**
     * @return units dropped into the network; zero when the block stays
     */
    long harvest(final ResourceFilter filter, final Storage network) {
        if (!space.isLoaded()) {
            return 0;
        }
        final ServerLevel level = space.level();
        final BlockPos pos = space.pos();
        final BlockState state = space.state();
        if (!isBreakable(state) || !filter.allows(asItem(state))) {
            return 0;
        }
        final FakePlayer player = space.player();
        final List<ResourceAmount> drops = dropsOf(Block.getDrops(state, level, pos, level.getBlockEntity(pos),
                player, tool.toStack(level)));
        if (!fitsInto(drops, network) || CommonHooks.fireBlockBreak(level, GameType.SURVIVAL, player, pos, state)
                .isCanceled()) {
            return 0;
        }
        level.destroyBlock(pos, false, player);
        return store(drops, network);
    }

    private boolean isBreakable(final BlockState state) {
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock)
                && state.getDestroySpeed(space.level(), space.pos()) >= 0
                && state.getBlock().asItem() != Items.AIR;
    }

    private static ItemKey asItem(final BlockState state) {
        final Item item = state.getBlock().asItem();
        return new ItemKey(ItemResource.of(item));
    }

    private static List<ResourceAmount> dropsOf(final List<ItemStack> stacks) {
        final ResourceCounter counter = new ResourceCounter();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                counter.add(ItemKey.of(stack), stack.getCount());
            }
        }
        return counter.contents();
    }

    private static boolean fitsInto(final List<ResourceAmount> drops, final Storage network) {
        for (ResourceAmount drop : drops) {
            if (network.insert(drop.resource(), drop.amount(), Action.SIMULATE, Actor.NOBODY) < drop.amount()) {
                return false;
            }
        }
        return true;
    }

    private long store(final List<ResourceAmount> drops, final Storage network) {
        long stored = 0;
        for (ResourceAmount drop : drops) {
            final long inserted = network.insert(drop.resource(), drop.amount(), Action.EXECUTE, Actor.NOBODY);
            stored += inserted;
            if (inserted < drop.amount() && drop.resource() instanceof ItemKey item) {
                dropInWorld(item, drop.amount() - inserted);
            }
        }
        return stored;
    }

    private void dropInWorld(final ItemKey item, final long amount) {
        final BlockPos pos = space.pos();
        long left = amount;
        while (left > 0) {
            final int count = (int) Math.min(left, item.maxStackSize());
            Containers.dropItemStack(space.level(), pos.getX(), pos.getY(), pos.getZ(), item.toStack(count));
            left -= count;
        }
    }
}
