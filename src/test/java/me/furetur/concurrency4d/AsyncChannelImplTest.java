package me.furetur.concurrency4d;

import org.junit.jupiter.api.Test;

class AsyncChannelImplTest {
    @Test
    void idDoesNotThrow() {
        new AsyncChannelImpl<>().id();
    }
}