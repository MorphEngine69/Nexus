package com.morphengine.nexus.storage;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.FilterMode;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.api.resource.ResourceFilter;
import com.morphengine.nexus.api.resource.ResourceKey;
import com.morphengine.nexus.api.storage.Actor;
import com.morphengine.nexus.api.storage.CellSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.ITEMS;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;

class ExternalStorageTest {

    private static final CellSpec ROOM = new CellSpec(4096, 8, 8, 8);
    private static final Actor MACHINE = () -> "machine";
    private static final Actor PLAYER = () -> "player";

    private final CellStorage chest = new CellStorage(ITEMS, ROOM, List.of());
    private final ExternalStorage external = new ExternalStorage(() -> chest);
    private final List<String> changes = new ArrayList<>();
    private final ExternalStorage.ChangeSink sink = (resource, delta) -> changes.add(resource + "" + delta);

    private void holdInChest(final ResourceKey resource, final long amount) {
        chest.insert(resource, amount, Action.EXECUTE, Actor.NOBODY);
    }

    @Test
    void whatTheBlockHoldsIsListedAfterTheFirstRescan() {
        holdInChest(STONE, 20);

        external.rescan(sink);

        assertThat(external.amountOf(STONE)).isEqualTo(20);
        assertThat(external.contents()).containsExactly(new ResourceAmount(STONE, 20));
        assertThat(changes).hasSize(1);
    }

    @Test
    void aRescanTellsOnlyWhatChangedSinceTheLast() {
        holdInChest(STONE, 20);
        external.rescan(sink);
        changes.clear();

        holdInChest(STONE, 5);
        holdInChest(DIRT, 3);
        external.rescan(sink);

        assertThat(changes).hasSize(2);
        assertThat(external.amountOf(STONE)).isEqualTo(25);
        assertThat(external.amountOf(DIRT)).isEqualTo(3);
    }

    @Test
    void whatLeavesTheBlockIsReportedAsALoss() {
        holdInChest(STONE, 20);
        external.rescan(sink);
        changes.clear();

        chest.extract(STONE, 20, Action.EXECUTE, Actor.NOBODY);
        external.rescan(sink);

        assertThat(external.amountOf(STONE)).isZero();
        assertThat(changes).hasSize(1);
        assertThat(changes.getFirst()).endsWith("-20");
    }

    @Test
    void aRescanWithNothingNewTellsNothing() {
        holdInChest(STONE, 20);
        external.rescan(sink);
        changes.clear();

        external.rescan(sink);

        assertThat(changes).isEmpty();
    }

    @Test
    void whatTheNetworkPutsInGoesIntoTheBlockAndIsCountedAtOnce() {
        final long inserted = external.insert(STONE, 30, Action.EXECUTE, Actor.NOBODY);

        assertThat(inserted).isEqualTo(30);
        assertThat(chest.amountOf(STONE)).isEqualTo(30);
        assertThat(external.amountOf(STONE)).isEqualTo(30);
        external.rescan(sink);
        assertThat(changes).isEmpty();
    }

    @Test
    void aSimulatedInsertMovesNothing() {
        assertThat(external.insert(STONE, 30, Action.SIMULATE, Actor.NOBODY)).isEqualTo(30);

        assertThat(chest.amountOf(STONE)).isZero();
        assertThat(external.amountOf(STONE)).isZero();
    }

    @Test
    void whatTheNetworkTakesOutLeavesTheBlockAndTheCopy() {
        holdInChest(STONE, 20);
        external.rescan(sink);

        assertThat(external.extract(STONE, 8, Action.EXECUTE, Actor.NOBODY)).isEqualTo(8);

        assertThat(chest.amountOf(STONE)).isEqualTo(12);
        assertThat(external.amountOf(STONE)).isEqualTo(12);
    }

    @Test
    void aReadOnlyBlockTakesNothingIn() {
        external.configure(ResourceFilter.NONE, ExternalAccess.READ_ONLY);

        assertThat(external.insert(STONE, 30, Action.EXECUTE, Actor.NOBODY)).isZero();
        assertThat(chest.amountOf(STONE)).isZero();
    }

    @Test
    void aReadOnlyBlockStillGivesItsContents() {
        holdInChest(STONE, 20);
        external.configure(ResourceFilter.NONE, ExternalAccess.READ_ONLY);
        external.rescan(sink);

        assertThat(external.extract(STONE, 20, Action.EXECUTE, Actor.NOBODY)).isEqualTo(20);
    }

    @Test
    void aWhitelistKeepsOtherResourcesOutOfTheListAndOutOfTheBlock() {
        holdInChest(STONE, 20);
        holdInChest(DIRT, 10);
        external.configure(new ResourceFilter(FilterMode.ALLOW, Set.of(STONE)), ExternalAccess.READ_WRITE);

        external.rescan(sink);

        assertThat(external.amountOf(STONE)).isEqualTo(20);
        assertThat(external.amountOf(DIRT)).isZero();
        assertThat(external.insert(DIRT, 5, Action.EXECUTE, Actor.NOBODY)).isZero();
        assertThat(external.extract(DIRT, 5, Action.EXECUTE, Actor.NOBODY)).isZero();
    }

    @Test
    void narrowingTheFilterReportsWhatDroppedOutOfSight() {
        holdInChest(STONE, 20);
        holdInChest(DIRT, 10);
        external.rescan(sink);
        changes.clear();

        external.configure(new ResourceFilter(FilterMode.DENY, Set.of(DIRT)), ExternalAccess.READ_WRITE);
        external.rescan(sink);

        assertThat(changes).hasSize(1);
        assertThat(changes.getFirst()).endsWith("-10");
    }

    @Test
    void aWhitelistedResourceIsWhereItShouldGoFirst() {
        external.configure(new ResourceFilter(FilterMode.ALLOW, Set.of(STONE)), ExternalAccess.READ_WRITE);

        assertThat(external.isReservedFor(STONE)).isTrue();
        assertThat(external.isReservedFor(DIRT)).isFalse();
    }

    private void limitMachinesToTen() {
        external.limit(resource -> 10, actor -> actor == MACHINE);
    }

    @Test
    void aLimitedActorMovesNoMoreThanTheLimitInATick() {
        limitMachinesToTen();

        assertThat(external.insert(STONE, 25, Action.EXECUTE, MACHINE)).isEqualTo(10);
        assertThat(external.insert(STONE, 25, Action.EXECUTE, MACHINE)).isZero();
    }

    @Test
    void whatWasMovedInAnEarlierTickIsForgotten() {
        limitMachinesToTen();
        external.insert(STONE, 25, Action.EXECUTE, MACHINE);

        external.startTick();

        assertThat(external.insert(STONE, 25, Action.EXECUTE, MACHINE)).isEqualTo(10);
    }

    @Test
    void takingOutCountsAgainstTheSameLimitAsPuttingIn() {
        holdInChest(STONE, 50);
        external.rescan(sink);
        limitMachinesToTen();

        assertThat(external.extract(STONE, 6, Action.EXECUTE, MACHINE)).isEqualTo(6);
        assertThat(external.insert(STONE, 20, Action.EXECUTE, MACHINE)).isEqualTo(4);
    }

    @Test
    void anActorThatIsNotLimitedMovesWhatItLikes() {
        limitMachinesToTen();

        assertThat(external.insert(STONE, 25, Action.EXECUTE, PLAYER)).isEqualTo(25);
    }

    @Test
    void aSimulatedMoveDoesNotUseUpTheLimit() {
        limitMachinesToTen();

        assertThat(external.insert(STONE, 25, Action.SIMULATE, MACHINE)).isEqualTo(10);
        assertThat(external.insert(STONE, 25, Action.EXECUTE, MACHINE)).isEqualTo(10);
    }

    @Test
    void aBlockThatIsGoneListsNothing() {
        final ExternalStorage gone = new ExternalStorage(() -> new CellStorage(ITEMS, ROOM, List.of()));

        gone.rescan(sink);

        assertThat(gone.contents()).isEmpty();
    }
}
