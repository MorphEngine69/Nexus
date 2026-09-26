package com.morphengine.nexus.api.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CellSpecTest {

    @Test
    void nonPositiveValuesAreRejected() {
        assertThatThrownBy(() -> new CellSpec(0, 8, 64, 8)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CellSpec(1024, 0, 64, 8)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CellSpec(1024, 8, 0, 8)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CellSpec(1024, 8, 64, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void typeLargerThanTheCellIsRejected() {
        assertThatThrownBy(() -> new CellSpec(64, 65, 1, 8)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statusCombinesBothLimits() {
        assertThat(CellStatus.of(false, false)).isEqualTo(CellStatus.HAS_ROOM);
        assertThat(CellStatus.of(true, false)).isEqualTo(CellStatus.TYPES_FULL);
        assertThat(CellStatus.of(false, true)).isEqualTo(CellStatus.BYTES_FULL);
        assertThat(CellStatus.of(true, true)).isEqualTo(CellStatus.FULL);
    }

    @Test
    void usageOutOfRangeIsRejected() {
        assertThatThrownBy(() -> new CellUsage(65, 64, 0, 4, CellStatus.HAS_ROOM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CellUsage(0, 64, 5, 4, CellStatus.HAS_ROOM))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
