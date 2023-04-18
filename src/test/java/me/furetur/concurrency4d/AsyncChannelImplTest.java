package me.furetur.concurrency4d;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsyncChannelImplTest {
    @Test
    void idDoesNotThrow() {
        new AsyncChannelImpl<>().id();
    }
}