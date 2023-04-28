package me.furetur.concurrency4d;

import org.junit.jupiter.api.Test;

class AsyncSelectTest {
    @Test
    void idDoesNotThrow() {
        var s = new AsyncSelect<>(new AsyncChannelImpl<>(), new AsyncChannelImpl<>());
        s.id();
    }
}