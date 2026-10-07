package com.morphengine.nexus.probe;

import com.morphengine.nexus.block.entity.ExternalVaultBlockEntity;
import com.morphengine.nexus.block.entity.StorageVaultBlockEntity;
import net.minecraft.network.chat.Component;

/**
 * What the Storage Vault and the External Vault show: where they rank and what they hold for the network.
 */
final class StorageProbes {

    private StorageProbes() {
    }

    static ProbeReport storageVault(final StorageVaultBlockEntity vault) {
        return ProbeReport.builder()
                .text(Component.translatable("tooltip.nexus.probe.cells", vault.storages().size(),
                        StorageVaultBlockEntity.SLOTS))
                .text(Component.translatable("gui.nexus.vault.priority", vault.storagePriority()))
                .build();
    }

    static ProbeReport externalVault(final ExternalVaultBlockEntity vault) {
        return ProbeReport.builder()
                .text(DeviceProbes.named("gui.nexus.external.access.", vault.settings().access()))
                .text(Component.translatable("gui.nexus.vault.priority", vault.storagePriority()))
                .build();
    }
}
