package com.morphengine.nexus.transfer;

import com.mojang.serialization.Codec;
import com.morphengine.nexus.api.network.DeviceRole;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.api.transport.TransferQuota;
import com.morphengine.nexus.filter.FilterSlots;
import com.morphengine.nexus.transport.PushEntry;
import com.morphengine.nexus.transport.PushTask;
import com.morphengine.nexus.transport.StorageRoute;
import com.morphengine.nexus.transport.SweepTask;
import com.morphengine.nexus.transport.TransferTask;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * The devices that move resources between their network and the block their
 * face touches: which way resources go and which settings their panel offers.
 */
public enum TransferKind implements StringRepresentable {

    /** Takes whatever its filter allows out of the block into the network. */
    PULLER(DeviceRole.PULLER) {
        @Override
        public TransferTask taskFor(
                final TransferSettings settings, final TransferQuota quota, final RandomGenerator random) {
            return new SweepTask(settings.filter().toResourceFilter(), quota);
        }

        @Override
        public StorageRoute route(final Storage beside, final Storage network, final Actor actor) {
            return new StorageRoute(beside, network, actor);
        }

        @Override
        public boolean hasDeliverySettings() {
            return false;
        }
    },

    /**
     * Delivers from the network into the block the resources its whitelist
     * lists, or everything but what its blacklist lists.
     */
    PUSHER(DeviceRole.PUSHER) {
        @Override
        public TransferTask taskFor(
                final TransferSettings settings, final TransferQuota quota, final RandomGenerator random) {
            if (settings.filter().mode() == FilterMode.DENY) {
                return new SweepTask(settings.filter().toResourceFilter(), quota);
            }
            final List<FilterSlots.Entry> listed = settings.filter().inSlotOrder();
            final List<PushEntry> entries = new ArrayList<>(listed.size());
            for (FilterSlots.Entry entry : listed) {
                entries.add(settings.delivery() == DeliveryMode.KEEP_STOCKED
                        ? new PushEntry(entry.resource(), settings.keepAmount(entry.slot(), entry.resource()))
                        : PushEntry.unlimited(entry.resource()));
            }
            return new PushTask(entries, settings.scheduling(), quota, random);
        }

        @Override
        public StorageRoute route(final Storage beside, final Storage network, final Actor actor) {
            return new StorageRoute(network, beside, actor);
        }

        @Override
        public boolean hasDeliverySettings() {
            return true;
        }
    };

    public static final Codec<TransferKind> CODEC = StringRepresentable.fromEnum(TransferKind::values);

    private final DeviceRole role;
    private final String serializedName = name().toLowerCase(Locale.ROOT);

    TransferKind(final DeviceRole role) {
        this.role = role;
    }

    public DeviceRole role() {
        return role;
    }

    /**
     * @param random picks where a random order starts
     * @return what one operation of such a device set to {@code settings} does
     */
    public abstract TransferTask taskFor(TransferSettings settings, TransferQuota quota, RandomGenerator random);

    /**
     * @param beside what the block the device's face touches offers on that face
     * @return the way resources go in an operation
     */
    public abstract StorageRoute route(Storage beside, Storage network, Actor actor);

    /**
     * @return whether the device has an order of delivery and amounts to keep
     *         stocked, both for the resources a whitelist lists
     */
    public abstract boolean hasDeliverySettings();

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
