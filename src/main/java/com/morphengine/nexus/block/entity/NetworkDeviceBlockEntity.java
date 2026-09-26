package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.NetworkLink;
import com.morphengine.nexus.level.NetworkMember;
import com.morphengine.nexus.menu.NetworkBadge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Base for the block entity of a network device with a panel: it knows its
 * network, carries a name, from the item it was placed from or given by the
 * player where the device is {@link Renamable}, and opens its menu on a new
 * click only. Designed for extension; subclasses that save more state call
 * {@code super.saveAdditional} and {@code super.loadAdditional}.
 */
public abstract class NetworkDeviceBlockEntity extends BlockEntity implements MenuHost, NetworkMember {

    private final ClickGuard clickGuard = new ClickGuard();
    private final NetworkLink network = new NetworkLink();
    private final DeviceName name = new DeviceName();

    protected NetworkDeviceBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void joinNetwork(final NetworkController joined) {
        network.join(joined);
    }

    @Override
    public void leaveNetwork(final NetworkController left) {
        network.leave(left);
    }

    /**
     * @return the Nexus of the device's network; {@code null} when none is connected
     */
    protected final @Nullable NetworkController controller() {
        return network.controller();
    }

    /**
     * @return whether the device is in a network whose energy pool is not empty
     */
    protected final boolean isNetworkPowered() {
        final NetworkController controller = network.controller();
        return controller != null && controller.energy().stored() > 0;
    }

    /**
     * @return name and color of the device's network; {@code null} when no Nexus is connected
     */
    public final @Nullable NetworkBadge networkBadge() {
        return NetworkBadge.of(network);
    }

    @Override
    public final Component getDisplayName() {
        return name.orDefault(getBlockState().getBlock().getName());
    }

    /**
     * @see Renamable#rename
     */
    protected final void changeName(final String newName) {
        name.rename(newName);
        setChanged();
    }

    @Override
    public final void markClosed() {
        clickGuard.markClosed(level);
    }

    @Override
    public final void markPlaced() {
        clickGuard.markPlaced(level);
    }

    @Override
    public final boolean ignoresClick() {
        return clickGuard.ignoresClick(level);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        name.save(output);
    }

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        name.load(input);
    }

    @Override
    protected final void applyImplicitComponents(final DataComponentGetter components) {
        super.applyImplicitComponents(components);
        name.applyFrom(components);
    }

    @Override
    protected final void collectImplicitComponents(final DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        name.collectInto(components);
    }

    @Override
    @SuppressWarnings("deprecation")
    public final void removeComponentsFromTag(final ValueOutput output) {
        DeviceName.removeFrom(output);
    }
}
