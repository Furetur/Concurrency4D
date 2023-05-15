package com.github.furetur.concurrency4d;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void valueMethodOfCloseMessageThrows() {
        assertThrows(NullPointerException.class, () -> Message.close().value());
    }

    @Test
    void toStringOfValue() {
        assertEquals("1", Message.value(1).toString());
    }

    @Test
    void toStringOfClose() {
        assertEquals("<CLOSE>", Message.close().toString());
    }
}