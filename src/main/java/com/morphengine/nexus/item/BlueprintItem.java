package com.morphengine.nexus.item;

import com.morphengine.nexus.api.automation.Blueprint;
import com.morphengine.nexus.api.automation.BlueprintInput;
import com.morphengine.nexus.api.automation.BlueprintKind;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.blueprint.EncodedBlueprint;
import com.morphengine.nexus.registry.NexusDataComponents;
import com.morphengine.nexus.resource.NexusResource;
import com.morphengine.nexus.resource.NexusResources;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * A Blueprint: blank, or with a recipe encoded in a Blueprint Terminal. An
 * encoded one lists what it takes and gives. Used in the air while sneaking,
 * a stack of encoded Blueprints is wiped blank again.
 */
public final class BlueprintItem extends Item {

    public BlueprintItem(final Item.Properties properties) {
        super(properties);
    }

    /**
     * @return the recipe encoded on {@code stack}; {@code null} for a blank Blueprint or another item
     */
    public static @Nullable EncodedBlueprint encodedOn(final ItemStack stack) {
        return stack.getItem() instanceof BlueprintItem ? stack.get(NexusDataComponents.ENCODED_BLUEPRINT.get()) : null;
    }

    public static boolean isBlank(final ItemStack stack) {
        return stack.getItem() instanceof BlueprintItem && encodedOn(stack) == null;
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() || encodedOn(stack) == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            stack.remove(NexusDataComponents.ENCODED_BLUEPRINT.get());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(final ItemStack stack) {
        final EncodedBlueprint encoded = encodedOn(stack);
        if (encoded == null) {
            return super.getName(stack);
        }
        final NexusResource product = NexusResources.of(encoded.blueprint().primaryOutput().resource());
        return Component.translatable("item.nexus.blueprint.encoded", product.name());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
            final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display,
            final Consumer<Component> builder, final TooltipFlag flag) {
        final EncodedBlueprint encoded = encodedOn(stack);
        if (encoded == null) {
            builder.accept(Component.translatable("tooltip.nexus.blueprint.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        final BlueprintKind kind = encoded.kind();
        builder.accept(Component.translatable("tooltip.nexus.blueprint." + kind.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.BLUE));
        final Blueprint blueprint = encoded.blueprint();
        if (encoded.substitution().isAllowed()) {
            builder.accept(Component.translatable("tooltip.nexus.blueprint.substitutes")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        builder.accept(Component.translatable("tooltip.nexus.blueprint.inputs").withStyle(ChatFormatting.GRAY));
        for (BlueprintInput input : blueprint.inputs()) {
            final MutableComponent line = entry(new ResourceAmount(input.preferred(), input.amount()));
            if (input.hasSubstitutes()) {
                line.append(Component.translatable("tooltip.nexus.blueprint.or_more", input.options().size() - 1));
            }
            builder.accept(line.withStyle(ChatFormatting.DARK_GRAY));
        }
        builder.accept(Component.translatable("tooltip.nexus.blueprint.outputs").withStyle(ChatFormatting.GRAY));
        for (ResourceAmount output : blueprint.outputs()) {
            builder.accept(entry(output).withStyle(ChatFormatting.DARK_GRAY));
        }
        builder.accept(Component.translatable("tooltip.nexus.blueprint.wipe").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent entry(final ResourceAmount amount) {
        final NexusResource resource = NexusResources.of(amount.resource());
        return Component.translatable("tooltip.nexus.blueprint.entry",
                resource.type().unit().quantity(amount.amount()), resource.name());
    }
}
