package com.morphengine.nexus.transfer;

import com.mojang.serialization.Codec;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.network.security.Permission;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.api.transport.TransferQuota;
import com.morphengine.nexus.transport.PullTask;
import com.morphengine.nexus.transport.PushTask;
import com.morphengine.nexus.transport.StorageRoute;
import com.morphengine.nexus.transport.SweepTask;
import com.morphengine.nexus.transport.TransferTask;
import com.morphengine.nexus.upgrade.UpgradeLimits;
import com.morphengine.nexus.upgrade.UpgradeTypes;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * The devices that sit against a block and move resources between their
 * network and what their face touches: Puller and Pusher with the block's own
 * storage, Placer and Remover with the block space itself. The kind decides
 * which way resources go, what one operation does, and which resources,
 * upgrades and settings its panel offers.
 */
public enum TransferKind implements StringRepresentable {

    /**
     * Takes whatever its filter allows out of the block into the network. Set
     * to keep stock, it takes only what the block holds beyond the amounts its
     * whitelist keeps.
     */
    PULLER(DeviceRole.PULLER, Permission.INSERT, TransferResource.ALL, Limits.STORAGE_DEVICE) {
        @Override
        public DeviceOperation operationFor(
                final TransferSettings settings, final TransferQuota quota, final RandomGenerator random) {
            final TransferTask task = pullTask(settings, quota);
            return place -> place.neighbour().isPresent()
                    ? task.runOnce(new StorageRoute(place.neighbour(), place.network(), place.actor())) : 0;
        }
    },

    /**
     * Delivers from the network into the block the resources its whitelist
     * lists, or everything but what its blacklist lists.
     */
    PUSHER(DeviceRole.PUSHER, Permission.EXTRACT, TransferResource.ALL, Limits.STORAGE_DEVICE) {
        @Override
        public DeviceOperation operationFor(
                final TransferSettings settings, final TransferQuota quota, final RandomGenerator random) {
            final TransferTask task = pushTask(settings, quota, random);
            return place -> place.neighbour().isPresent()
                    ? task.runOnce(new StorageRoute(place.network(), place.neighbour(), place.actor())) : 0;
        }

        @Override
        public boolean hasScheduling() {
            return true;
        }

        @Override
        public boolean ordersCrafts() {
            return true;
        }
    },

    /**
     * Places blocks from the network in the space its face touches, as its
     * whitelist lists them or anything its blacklist does not, or drops them
     * there as items; pours fluids there as sources.
     */
    PLACER(DeviceRole.PUSHER, Permission.EXTRACT, TransferResource.MATERIAL, Limits.PLACER) {
        @Override
        public DeviceOperation operationFor(
                final TransferSettings settings, final TransferQuota quota, final RandomGenerator random) {
            final TransferTask task = pushTask(settings, quota, random);
            return place -> task.runOnce(new StorageRoute(place.network(),
                    place.front().placement(settings.worldMode()), place.actor()));
        }

        @Override
        public boolean hasScheduling() {
            return true;
        }

        @Override
        public boolean hasWorldMode() {
            return true;
        }

        @Override
        public boolean ordersCrafts() {
            return true;
        }
    },

    /**
     * Breaks the block its face touches into the network, as its filter
     * allows the block as an item, or picks up the items lying there; takes
     * fluid sources there.
     */
    REMOVER(DeviceRole.PULLER, Permission.INSERT, TransferResource.MATERIAL, Limits.REMOVER) {
        @Override
        public DeviceOperation operationFor(
                final TransferSettings settings, final TransferQuota quota, final RandomGenerator random) {
            final ResourceFilter filter = settings.filter().toResourceFilter(settings.matchMode());
            if (settings.resource() == TransferResource.ITEM && settings.worldMode() == WorldMode.BLOCKS) {
                return place -> place.front().harvest(filter, place.network(), place.actor());
            }
            final TransferTask task = new SweepTask(filter, quota);
            return place -> task.runOnce(new StorageRoute(sourceIn(place, settings), place.network(),
                    place.actor()));
        }

        @Override
        public boolean hasWorldMode() {
            return true;
        }

        private static Storage sourceIn(final Workplace place, final TransferSettings settings) {
            return settings.resource() == TransferResource.FLUID
                    ? place.front().fluidSource() : place.front().groundItems();
        }
    };

    public static final Codec<TransferKind> CODEC = StringRepresentable.fromEnum(TransferKind::values);

    private final DeviceRole role;
    private final Permission permission;
    private final List<TransferResource> resources;
    private final UpgradeLimits upgradeLimits;
    private final String serializedName = name().toLowerCase(Locale.ROOT);

    TransferKind(final DeviceRole role, final Permission permission, final List<TransferResource> resources,
                 final UpgradeLimits upgradeLimits) {
        this.role = role;
        this.permission = permission;
        this.resources = resources;
        this.upgradeLimits = upgradeLimits;
    }

    public DeviceRole role() {
        return role;
    }

    /**
     * @return what the device's owner must be allowed in its network for the
     *         device to work: putting into the network for a device that takes
     *         in, taking out of it for one that gives out
     */
    public Permission permission() {
        return permission;
    }

    /**
     * @return the kinds of resource the device can be set to move, in the order its button steps through them
     */
    public List<TransferResource> resources() {
        return resources;
    }

    /**
     * @return which upgrades the device takes and how many of each
     */
    public UpgradeLimits upgradeLimits() {
        return upgradeLimits;
    }

    /**
     * @param random picks where a random order starts
     * @return what one operation of such a device set to {@code settings} does
     */
    public abstract DeviceOperation operationFor(TransferSettings settings, TransferQuota quota,
                                                 RandomGenerator random);

    /**
     * @return whether the device delivers the resources its whitelist lists in
     *         an order that can be chosen
     */
    public boolean hasScheduling() {
        return false;
    }

    /**
     * @return whether the device can be set to work with blocks or with items
     *         lying loose; see {@link WorldMode}
     */
    public boolean hasWorldMode() {
        return false;
    }

    /**
     * @return whether an Autocrafting Upgrade makes the device order what it
     *         would deliver and the network lacks
     */
    public boolean ordersCrafts() {
        return false;
    }

    /**
     * @param backwards step to the previous kind instead of the next
     * @return the kind of resource after {@code current} among those the device
     *         moves, or the first of them when it moves no such kind
     */
    public TransferResource stepResource(final TransferResource current, final boolean backwards) {
        final int index = resources.indexOf(current);
        if (index < 0) {
            return resources.getFirst();
        }
        final int offset = backwards ? resources.size() - 1 : 1;
        return resources.get((index + offset) % resources.size());
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    private static TransferTask pullTask(final TransferSettings settings, final TransferQuota quota) {
        if (settings.delivery() == DeliveryMode.KEEP_STOCKED && settings.filter().mode() == FilterMode.ALLOW) {
            return new PullTask(settings.stock(), quota);
        }
        return new SweepTask(settings.filter().toResourceFilter(settings.matchMode()), quota);
    }

    private static TransferTask pushTask(
            final TransferSettings settings, final TransferQuota quota, final RandomGenerator random) {
        if (settings.filter().mode() == FilterMode.DENY) {
            return new SweepTask(settings.filter().toResourceFilter(settings.matchMode()), quota);
        }
        return new PushTask(settings.deliveries(), settings.scheduling(), quota, random);
    }

    /**
     * Upgrade limits of the kinds; placeholder balance. Several copies of a
     * kind share one slot, up to the limit.
     */
    private static final class Limits {

        /**
         * A Puller or Pusher: up to four Speed Upgrades, a Stack, an Efficiency, a
         * Regulator, an Autocrafting and a Chunk Loader Upgrade, and up to three
         * Capacity Upgrades.
         */
        static final UpgradeLimits STORAGE_DEVICE = new UpgradeLimits(Map.of(
                UpgradeTypes.SPEED, 4, UpgradeTypes.STACK, 1, UpgradeTypes.REGULATOR, 1, UpgradeTypes.CAPACITY, 3,
                UpgradeTypes.AUTOCRAFTING, 1, UpgradeTypes.CHUNK_LOADER, 1, UpgradeTypes.EFFICIENCY, 1));
        /** A Placer keeps no stock in the world, so it takes no Regulator. */
        static final UpgradeLimits PLACER = new UpgradeLimits(Map.of(
                UpgradeTypes.SPEED, 4, UpgradeTypes.STACK, 1, UpgradeTypes.CAPACITY, 3, UpgradeTypes.AUTOCRAFTING, 1,
                UpgradeTypes.CHUNK_LOADER, 1, UpgradeTypes.EFFICIENCY, 1));
        /** A Remover breaks with Fortune of up to level three, or with Silk Touch. */
        static final UpgradeLimits REMOVER = new UpgradeLimits(Map.of(
                UpgradeTypes.SPEED, 4, UpgradeTypes.STACK, 1, UpgradeTypes.CAPACITY, 3, UpgradeTypes.FORTUNE, 3,
                UpgradeTypes.SILK_TOUCH, 1, UpgradeTypes.CHUNK_LOADER, 1,
                UpgradeTypes.EFFICIENCY, 1));

        private Limits() {
        }
    }
}
