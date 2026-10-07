package com.morphengine.nexus.api.network;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkStatisticsTest {

    @Test
    void countsDevicesOfARole() {
        final NetworkStatistics statistics = new NetworkStatistics(3, Map.of(DeviceRole.PULLER, 2), 0, 0, 0, 0);

        assertThat(statistics.count(DeviceRole.PULLER)).isEqualTo(2);
    }

    @Test
    void roleLeftOutCountsZero() {
        assertThat(NetworkStatistics.EMPTY.count(DeviceRole.PUSHER)).isZero();
    }

    @Test
    void negativeCountOfARoleIsRejected() {
        assertThatThrownBy(() -> new NetworkStatistics(0, Map.of(DeviceRole.STORAGE, -1), 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
