package com.morphengine.nexus.api.upgrade;

/**
 * A kind of upgrade a device can hold, such as speed: Nexus registers its own,
 * an addon registers more. Each kind is a single registered instance, so kinds
 * are compared by identity. A kind has no effect of its own; the device that
 * holds it decides what it does and how many copies of it count.
 */
public interface UpgradeType {
}
