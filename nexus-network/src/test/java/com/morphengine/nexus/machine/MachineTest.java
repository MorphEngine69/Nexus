package com.morphengine.nexus.machine;

import com.morphengine.nexus.api.core.Action;
import com.morphengine.nexus.api.resource.ResourceAmount;
import com.morphengine.nexus.energy.BufferUpgrades;
import com.morphengine.nexus.energy.EfficiencyUpgrades;
import com.morphengine.nexus.transport.SideMode;
import org.junit.jupiter.api.Test;

import static com.morphengine.nexus.api.storage.Actor.NOBODY;
import static com.morphengine.nexus.machine.MachineTestRecipes.STONE_ENERGY;
import static com.morphengine.nexus.machine.MachineTestRecipes.STONE_TICKS;
import static com.morphengine.nexus.test.TestResources.DIRT;
import static com.morphengine.nexus.test.TestResources.STONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MachineTest {

    private static final long LIMIT = 64;

    private final Machine machine = new Machine(MachineTier.BASIC, MachineTestRecipes.ALL,
            new PlainMachineSlots(resource -> LIMIT));

    private void charge(final long amount) {
        machine.energy().insert(amount, Action.EXECUTE);
    }

    @Test
    void aNewMachineHasTheLinesAndBufferOfItsTier() {
        assertThat(machine.lineCount()).isEqualTo(MachineTier.BASIC.linePairs());
        assertThat(machine.energy().capacity()).isEqualTo(MachineTier.BASIC.bufferCapacity());
        assertThat(machine.activity()).isEqualTo(MachineActivity.IDLE);
    }

    @Test
    void theMachineWorksAtTheSpeedOfItsTier() {
        charge(MachineTier.BASIC.maxInsert());
        machine.inventory().insert(STONE, 1, Action.EXECUTE, NOBODY);
        int ticks = 0;

        while (machine.inventory().output(0).isEmpty()) {
            machine.tick();
            ticks++;
        }

        assertThat(ticks).isEqualTo((int) Math.ceil(STONE_TICKS * 100.0 / MachineTier.BASIC.speedPercent()));
    }

    @Test
    void everyTierAfterTheFirstIsFasterThanARecipeSays() {
        for (int rank = 2; rank <= 4; rank++) {
            assertThat(MachineTier.ofRank(rank).speedPercent()).isGreaterThan(MachineTier.BASIC.speedPercent());
        }
    }

    private long energySpentOnOneStone(final Machine worker) {
        worker.energy().insert(MachineTier.BASIC.maxInsert(), Action.EXECUTE);
        worker.inventory().insert(STONE, 1, Action.EXECUTE, NOBODY);
        while (worker.inventory().output(0).isEmpty()) {
            worker.tick();
        }
        return worker.energy().totalExtracted();
    }

    @Test
    void efficiencyUpgradesMakeTheWorkCheaper() {
        final Machine thrifty = new Machine(MachineTier.BASIC, MachineTestRecipes.ALL,
                new PlainMachineSlots(resource -> LIMIT));
        thrifty.setEfficiencyUpgrades(EfficiencyUpgrades.MAX_UPGRADES);

        assertThat(energySpentOnOneStone(thrifty)).isLessThan(energySpentOnOneStone(machine));
    }

    @Test
    void bufferUpgradesEnlargeTheBufferAndKeepWhatItHolds() {
        charge(100);

        machine.setBufferUpgrades(BufferUpgrades.MAX_UPGRADES);

        assertThat(machine.energy().capacity()).isEqualTo(MachineTier.BASIC.bufferCapacity() * 2);
        assertThat(machine.bufferCapacity()).isEqualTo(machine.energy().capacity());
        assertThat(machine.energy().stored()).isEqualTo(100);
    }

    @Test
    void aTierUpgradeKeepsTheBufferUpgrades() {
        machine.setBufferUpgrades(1);

        machine.upgradeTo(MachineTier.ADVANCED);

        assertThat(machine.energy().capacity()).isEqualTo(MachineTier.ADVANCED.bufferCapacity() * 2);
    }

    @Test
    void moreEfficiencyOrBufferUpgradesThanTheLimitAreRefused() {
        assertThatThrownBy(() -> machine.setEfficiencyUpgrades(EfficiencyUpgrades.MAX_UPGRADES + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> machine.setBufferUpgrades(BufferUpgrades.MAX_UPGRADES + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void speedUpgradesMakeTheMachineFaster() {
        final int plain = machine.speedPercent();

        machine.setSpeedUpgrades(MachineSpeed.MAX_SPEED_UPGRADES);

        assertThat(machine.speedPercent()).isGreaterThan(plain);
        assertThat(machine.speedUpgrades()).isEqualTo(MachineSpeed.MAX_SPEED_UPGRADES);
    }

    @Test
    void moreSpeedUpgradesThanTheLimitAreRefused() {
        assertThatThrownBy(() -> machine.setSpeedUpgrades(MachineSpeed.MAX_SPEED_UPGRADES + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(machine.speedUpgrades()).isZero();
    }

    @Test
    void theMachineShowsTheMostImportantThingItsLinesDo() {
        machine.upgradeTo(MachineTier.ADVANCED);
        charge(STONE_ENERGY * 3);
        machine.inventory().insert(STONE, 1, Action.EXECUTE, NOBODY);
        machine.inventory().insert(DIRT, 5, Action.EXECUTE, NOBODY);

        assertThat(machine.tick()).isEqualTo(MachineActivity.WORKING);
        assertThat(machine.activity()).isEqualTo(MachineActivity.WORKING);
    }

    @Test
    void aMachineWithoutFeWaitsForEnergy() {
        machine.inventory().insert(STONE, 1, Action.EXECUTE, NOBODY);

        assertThat(machine.tick()).isEqualTo(MachineActivity.WAITING_FOR_ENERGY);
    }

    @Test
    void severalLinesWorkAtTheSameTimeOnTheSameBuffer() {
        machine.upgradeTo(MachineTier.ADVANCED);
        charge(MachineTier.ADVANCED.maxInsert());
        machine.inventory().insert(STONE, 1, Action.EXECUTE, NOBODY);
        machine.inventory().insert(DIRT, 2, Action.EXECUTE, NOBODY);
        final long before = machine.energy().stored();

        machine.tick();

        assertThat(before - machine.energy().stored()).isGreaterThan(
                Math.ceilDiv(STONE_ENERGY * machine.speedPercent(), 100L));
        assertThat(machine.line(0).isRunning()).isTrue();
        assertThat(machine.line(1).isRunning()).isTrue();
    }

    @Test
    void upgradingKeepsTheContentsAndTheEnergyAndGrowsEverything() {
        charge(500);
        machine.inventory().insert(STONE, 7, Action.EXECUTE, NOBODY);

        machine.upgradeTo(MachineTier.ADVANCED);

        assertThat(machine.tier()).isEqualTo(MachineTier.ADVANCED);
        assertThat(machine.lineCount()).isEqualTo(MachineTier.ADVANCED.linePairs());
        assertThat(machine.inventory().input(0).contents()).contains(new ResourceAmount(STONE, 7));
        assertThat(machine.energy().stored()).isEqualTo(500);
        assertThat(machine.energy().capacity()).isEqualTo(MachineTier.ADVANCED.bufferCapacity());
    }

    @Test
    void upgradingToTheSameOrALowerTierIsRefused() {
        assertThatThrownBy(() -> machine.upgradeTo(MachineTier.BASIC)).isInstanceOf(IllegalArgumentException.class);
        machine.upgradeTo(MachineTier.ADVANCED);
        assertThatThrownBy(() -> machine.upgradeTo(MachineTier.BASIC)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theBufferTakesEnergyInUpToItsRate() {
        final long accepted = machine.energy().insert(MachineTier.BASIC.maxInsert() * 10, Action.EXECUTE);

        assertThat(accepted).isEqualTo(MachineTier.BASIC.maxInsert());
    }

    @Test
    void byDefaultTheTopLetsThingsInAndTheBottomLetsThemOut() {
        assertThat(machine.insertFrom(MachineSide.TOP, STONE, 5, Action.EXECUTE, NOBODY)).isEqualTo(5);
        machine.inventory().output(0).restore(DIRT, 3);

        assertThat(machine.insertFrom(MachineSide.BOTTOM, STONE, 5, Action.EXECUTE, NOBODY)).isZero();
        assertThat(machine.extractFrom(MachineSide.TOP, DIRT, 3, Action.EXECUTE, NOBODY)).isZero();
        assertThat(machine.extractFrom(MachineSide.BOTTOM, DIRT, 3, Action.EXECUTE, NOBODY)).isEqualTo(3);
    }

    @Test
    void theBackLetsThingsBothWayAndAClosedSideNeither() {
        assertThat(machine.sides().mode(MachineSide.BACK)).isEqualTo(SideMode.BOTH);
        machine.setSideMode(MachineSide.BACK, SideMode.CLOSED);

        assertThat(machine.insertFrom(MachineSide.BACK, STONE, 5, Action.EXECUTE, NOBODY)).isZero();
        assertThat(machine.extractFrom(MachineSide.BACK, DIRT, 5, Action.EXECUTE, NOBODY)).isZero();
    }

    @Test
    void theInventoryItselfIsNotBoundByTheSides() {
        machine.setSideMode(MachineSide.TOP, SideMode.CLOSED);

        assertThat(machine.inventory().insert(STONE, 5, Action.EXECUTE, NOBODY)).isEqualTo(5);
    }

    @Test
    void savedSidesComeBack() {
        machine.setSideMode(MachineSide.TOP, SideMode.OUTPUT);
        machine.setSideMode(MachineSide.LEFT, SideMode.CLOSED);
        final int saved = machine.sides().toBits();
        final Machine other = new Machine(MachineTier.BASIC, MachineTestRecipes.ALL,
                new PlainMachineSlots(resource -> LIMIT));

        other.restoreSides(saved);

        assertThat(other.sides()).isEqualTo(machine.sides());
    }
}
