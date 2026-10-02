package com.morphengine.nexus.transfer;

import com.morphengine.nexus.api.storage.Storage;
import com.morphengine.nexus.level.SideStorage;
import com.morphengine.nexus.world.FrontSpace;

/**
 * Where an attached device works in one operation.
 *
 * @param network   the network's resources of the kind the device moves
 * @param neighbour what the block its face touches offers on the side it is
 *                  touched, of the kind it moves; what a Puller and a Pusher work with
 * @param front     the block space its face touches; what a Placer and a Remover work in
 */
public record Workplace(Storage network, SideStorage neighbour, FrontSpace front) {
}
