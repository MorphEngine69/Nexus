package com.morphengine.nexus.block.entity;

import com.morphengine.nexus.access.NameAndId;
import com.morphengine.nexus.access.NetworkSecurityData;
import com.morphengine.nexus.access.PlayerPlaced;
import com.morphengine.nexus.access.Secured;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.security.AccessPolicy;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.block.NetworkBlock;
import com.morphengine.nexus.energy.DeviceEnergyMeter;
import com.morphengine.nexus.level.DeviceActor;
import com.morphengine.nexus.level.DeviceEnergyRow;
import com.morphengine.nexus.level.MeteredDevice;
import com.morphengine.nexus.level.NetworkController;
import com.morphengine.nexus.level.NetworkLink;
import com.morphengine.nexus.level.NetworkMember;
import com.morphengine.nexus.menu.NetworkBadge;
import com.morphengine.nexus.nbt.ValueInput;
import com.morphengine.nexus.nbt.ValueOutput;
import com.morphengine.nexus.security.Member;
import com.morphengine.nexus.security.NetworkSecurity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Base for the block entity of a network device with a panel: it knows its
 * network, carries a name, from the item it was placed from or given by the
 * player where the device is {@link Renamable}, and opens its menu on a new
 * click only. Designed for extension; subclasses that save more state call
 * {@code super.saveAdditional} and {@code super.loadAdditional}.
 *
 * <p>A device works for the player who placed it, its owner: what it does
 * with its network on its own is subject to the owner's access rights there
 * ({@link #actor()}, {@link #ownerMay}). A device from before devices had
 * owners takes the owner of the first network it joins. Who may open or change
 * the device is up to the rules of its network, or, while it is in none, of
 * the network it was last in, and failing that up to its owner alone.
 */
public abstract class NetworkDeviceBlockEntity extends BlockEntity
        implements MenuHost, NetworkMember, MeteredDevice, Secured, PlayerPlaced, RemovalEffects {

    private final ClickGuard clickGuard = new ClickGuard();
    private final NetworkLink network = new NetworkLink();
    private final DeviceName name = new DeviceName();
    private final List<ItemComponentPart> itemParts = new ArrayList<>();
    private final DeviceOwner owner = new DeviceOwner();
    private final DeviceEnergyMeter energyMeter = new DeviceEnergyMeter();
    private final AccessPolicy ownerOnly = (player, permission) -> owner.isOwnerOrNobody(player);
    /** Built when first needed, again after the owner or the name changes. */
    private @Nullable DeviceActor actor;
    private @Nullable Permission missing;

    protected NetworkDeviceBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public final DeviceEnergyMeter energyMeter() {
        return energyMeter;
    }

    @Override
    public final DeviceEnergyRow energyRow(final long gameTime) {
        final DeviceRole role = getBlockState().getBlock() instanceof NetworkBlock block
                ? block.role() : DeviceRole.OTHER;
        final Level here = Objects.requireNonNull(level, "a device in a network stands in a level");
        return new DeviceEnergyRow(GlobalPos.of(here.dimension(), worldPosition), getDisplayName(), role,
                energyMeter.use(gameTime));
    }

    @Override
    public void joinNetwork(final NetworkController joined) {
        network.join(joined);
        boolean changed = owner.remember(joined.network().id());
        if (!owner.hasOwner()) {
            final NetworkSecurity security = joined.security();
            final Optional<Member> networkOwner = security.owner().flatMap(security::member);
            if (networkOwner.isPresent() && owner.claimFor(networkOwner.get().id(), networkOwner.get().name())) {
                actor = null;
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    @Override
    public void leaveNetwork(final NetworkController left) {
        network.leave(left);
    }

    @Override
    public void placedBy(final Player player) {
        owner.placedBy(player.getUUID(), player.getName().getString());
        actor = null;
        setChanged();
    }

    @Override
    public final AccessPolicy accessPolicy() {
        final NetworkController controller = network.controller();
        if (controller != null) {
            return controller.security();
        }
        final UUID last = owner.lastNetwork();
        final MinecraftServer server = level != null ? level.getServer() : null;
        final NetworkSecurity remembered = last != null && server != null
                ? NetworkSecurityData.of(server).find(last) : null;
        return remembered != null ? remembered : ownerOnly;
    }

    @Override
    public final boolean isOwnedBy(final UUID player) {
        return owner.isOwnedBy(player);
    }

    /**
     * @return who the device's own work is done for, to hand to the network's storage
     */
    protected final DeviceActor actor() {
        DeviceActor current = actor;
        if (current == null) {
            current = owner.actorNamed(getDisplayName().getString());
            actor = current;
        }
        return current;
    }

    /**
     * Cheap enough for every operation.
     *
     * @return whether the device's owner may do what takes {@code permission}
     *         where the device is; a device nobody owns may do anything
     */
    protected final boolean ownerMay(final Permission permission) {
        final UUID ownerId = actor().owner();
        return ownerId == null || accessPolicy().isAllowed(ownerId, permission);
    }

    /**
     * @return the id and name of the player the device works for, as last seen;
     *         {@code null} when it works for nobody
     */
    public final @Nullable NameAndId ownerProfile() {
        return owner.profile();
    }

    /**
     * Notes what keeps the device from working on its own: the permission its
     * owner lacks, or {@code null} when nothing does. Not saved; the device
     * finds out again as it works.
     */
    protected final void markHalted(final @Nullable Permission lacking) {
        missing = lacking;
    }

    /**
     * @return the permission the device's owner lacks for the device to work,
     *         as last found; {@code null} while it works
     */
    public final @Nullable Permission missingPermission() {
        return missing;
    }

    /**
     * @return the Nexus of the device's network; {@code null} when none is connected
     */
    public final @Nullable NetworkController controller() {
        return network.controller();
    }

    /**
     * @return whether the device is in a network whose energy pool is not empty
     */
    public final boolean isNetworkPowered() {
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
        actor = null;
        setChanged();
    }

    @Override
    public final ClickGuard clickGuard() {
        return clickGuard;
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueOutput output = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        name.save(output);
        owner.save(output);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ValueInput input = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        name.load(input);
        owner.load(input);
        actor = null;
    }

    @Override
    protected final void applyImplicitComponents(final BlockEntity.DataComponentInput components) {
        super.applyImplicitComponents(components);
        final ComponentSource source = new InputSource(components);
        name.applyFrom(source);
        for (ItemComponentPart part : itemParts) {
            part.applyFrom(source);
        }
    }

    @Override
    protected final void collectImplicitComponents(final DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        name.collectInto(components);
        for (ItemComponentPart part : itemParts) {
            part.collectInto(components);
        }
    }

    /**
     * Has the device carry {@code part} on its item besides its name. Called once for each part, by the constructor
     * of the device.
     */
    protected final void carryOnItem(final ItemComponentPart part) {
        itemParts.add(Objects.requireNonNull(part, "part must not be null"));
    }

    /**
     * Called before the block of the device is broken or replaced, to drop what it holds and let go of what it uses.
     */
    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        // Nothing to do: the devices that hold something add their own.
    }

    @Override
    @SuppressWarnings("deprecation")
    public final void removeComponentsFromTag(final CompoundTag tag) {
        DeviceName.removeFrom(tag);
    }

    private record InputSource(BlockEntity.DataComponentInput components) implements ComponentSource {

        @Override
        public <T> @Nullable T get(final DataComponentType<? extends T> type) {
            return components.get(type);
        }

        @Override
        public <T> T getOrDefault(final DataComponentType<? extends T> type, final T fallback) {
            return components.getOrDefault(type, fallback);
        }
    }
}
