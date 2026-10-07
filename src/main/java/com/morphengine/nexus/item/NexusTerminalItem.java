package com.morphengine.nexus.item;

import com.morphengine.nexus.access.NetworkAccess;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.NetworkDirectory;
import com.morphengine.nexus.menu.PortableTerminals;
import com.morphengine.nexus.menu.TerminalSlot;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.terminal.EnumCycle;
import com.morphengine.nexus.terminal.TerminalKind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * A Nexus Terminal: a terminal to carry. A right click on a Nexus binds it to
 * that network; a right click in the air opens it wherever a Nexus Link of the
 * network reaches. It works as a terminal, a crafting terminal or a blueprint
 * terminal, as its mode says; used while sneaking in the air, it switches
 * mode. It can be opened and switched from wherever it is carried, with keys.
 *
 * <p>It is bound to the network by id, not to the Nexus by name or place: the
 * network stays reachable when renamed and when its Nexus is moved. Only when
 * no Nexus of that network stands anywhere any more does it fall back to the
 * place it last saw the Nexus at, so a Nexus broken and replaced on the spot,
 * even by a new one, still works with it.
 */
public final class NexusTerminalItem extends Item {

    public NexusTerminalItem(final Item.Properties properties) {
        super(properties);
    }

    /**
     * @return where the Nexus {@code stack} is bound to stood when last seen;
     *         {@code null} when it is bound to none, or no Nexus Terminal
     */
    public static @Nullable GlobalPos boundNexus(final ItemStack stack) {
        return stack.getItem() instanceof NexusTerminalItem ? stack.get(NexusDataComponents.BOUND_NEXUS.get()) : null;
    }

    /**
     * @return the id of the network {@code stack} is bound to; {@code null} when
     *         it is bound to none, was bound before networks had ids, or is no Nexus Terminal
     */
    public static @Nullable UUID boundNetwork(final ItemStack stack) {
        return stack.getItem() instanceof NexusTerminalItem ? stack.get(NexusDataComponents.BOUND_NETWORK.get()) : null;
    }

    /**
     * @return the Nexus of the network {@code stack} is bound to, while it stands
     *         in a loaded chunk; {@code null} otherwise. Server side only.
     */
    public static @Nullable NetworkController nexusOf(final ItemStack stack, final MinecraftServer server) {
        return controllerAt(whereNexusOf(stack, server), server);
    }

    private static @Nullable NetworkController controllerAt(
            final @Nullable GlobalPos nexus, final MinecraftServer server) {
        final ServerLevel level = nexus != null ? server.getLevel(nexus.dimension()) : null;
        if (nexus == null || level == null || !level.isLoaded(nexus.pos())) {
            return null;
        }
        return level.getBlockEntity(nexus.pos()) instanceof NetworkController controller && !controller.isRemoved()
                ? controller : null;
    }

    /**
     * @return where the Nexus of the bound network stands, as the directory
     *         knows it; where the Nexus was last seen when the directory knows
     *         none, or the terminal was bound before networks had ids
     */
    private static @Nullable GlobalPos whereNexusOf(final ItemStack stack, final MinecraftServer server) {
        final UUID network = boundNetwork(stack);
        final GlobalPos known = network != null ? NetworkDirectory.of(server).nexusOf(network) : null;
        return known != null ? known : boundNexus(stack);
    }

    /**
     * Binds {@code stack} to the network of {@code nexus}, which stands at {@code pos}.
     */
    private static void bind(final ItemStack stack, final NetworkController nexus, final GlobalPos pos) {
        stack.set(NexusDataComponents.BOUND_NETWORK.get(), nexus.network().id());
        stack.set(NexusDataComponents.BOUND_NEXUS.get(), pos);
    }

    /**
     * @return what the terminal works as; a plain terminal until the mode is switched
     */
    public static TerminalKind modeOf(final ItemStack stack) {
        return stack.getOrDefault(NexusDataComponents.TERMINAL_MODE.get(), TerminalKind.TERMINAL);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof NetworkController nexus)) {
            return InteractionResult.PASS;
        }
        final Player player = context.getPlayer();
        if (player != null && !NetworkAccess.permits(player, nexus, Permission.OPEN)) {
            NetworkAccess.refuse(player, Permission.OPEN);
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            bind(context.getItemInHand(), nexus, GlobalPos.of(level.dimension(), pos.immutable()));
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("item.nexus.nexus_terminal.bound"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                cycleMode(player, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return open(serverPlayer, new TerminalSlot.Hand(hand)) ? InteractionResult.SUCCESS
                    : InteractionResult.FAIL;
        }
        return boundNexus(stack) == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }

    /**
     * Opens the terminal in {@code slot} for {@code player}; tells them when it is bound to no network. Server side
     * only.
     *
     * @return whether the terminal was bound, so that its menu was opened or refused for lack of access
     */
    public static boolean open(final ServerPlayer player, final TerminalSlot slot) {
        final ItemStack stack = slot.stackOf(player);
        if (boundNexus(stack) == null) {
            player.sendOverlayMessage(Component.translatable("item.nexus.nexus_terminal.unbound"));
            return false;
        }
        refreshBinding(stack, player.level().getServer());
        PortableTerminals.open(player, slot);
        return true;
    }

    /**
     * Switches {@code stack} to the next mode and tells {@code player}. Server side only.
     */
    public static void cycleMode(final Player player, final ItemStack stack) {
        final TerminalKind next = EnumCycle.step(modeOf(stack), false);
        stack.set(NexusDataComponents.TERMINAL_MODE.get(), next);
        player.sendOverlayMessage(Component.translatable("item.nexus.nexus_terminal.mode",
                Component.translatable(modeKey(next))));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display,
            final Consumer<Component> builder, final TooltipFlag flag) {
        builder.accept(Component.translatable("item.nexus.nexus_terminal.mode",
                Component.translatable(modeKey(modeOf(stack)))).withStyle(ChatFormatting.GRAY));
        final GlobalPos nexus = boundNexus(stack);
        if (nexus == null) {
            builder.accept(Component.translatable("tooltip.nexus.nexus_terminal.unbound")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        final BlockPos pos = nexus.pos();
        builder.accept(Component.translatable("tooltip.nexus.nexus_terminal.bound", pos.getX(), pos.getY(),
                pos.getZ()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.literal(nexus.dimension().identifier().toString())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * Binds {@code stack} again to the Nexus it finds, so that it keeps up with
     * a Nexus that moved and takes over the network of one that replaced the
     * bound Nexus on the spot.
     */
    private static void refreshBinding(final ItemStack stack, final MinecraftServer server) {
        final GlobalPos where = whereNexusOf(stack, server);
        final NetworkController nexus = controllerAt(where, server);
        if (where != null && nexus != null) {
            bind(stack, nexus, where);
        }
    }

    private static String modeKey(final TerminalKind mode) {
        return "block.nexus." + mode.getSerializedName();
    }
}
