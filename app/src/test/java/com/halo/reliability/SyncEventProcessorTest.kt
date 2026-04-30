package com.halo.reliability

import com.halo.data.matrix.SyncEventProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SyncEventProcessorTest {
    @Test
    fun buildDeterministicEventKey_sameInput_sameKey() {
        val first = SyncEventProcessor.buildDeterministicEventKey(
            roomId = "!room:example.org",
            senderId = "@alice:example.org",
            timestamp = 1714500000000L,
            contentHash = 42
        )
        val second = SyncEventProcessor.buildDeterministicEventKey(
            roomId = "!room:example.org",
            senderId = "@alice:example.org",
            timestamp = 1714500000000L,
            contentHash = 42
        )

        assertEquals(first, second)
    }

    @Test
    fun buildDeterministicEventKey_differentInput_differentKey() {
        val first = SyncEventProcessor.buildDeterministicEventKey(
            roomId = "!roomA:example.org",
            senderId = "@alice:example.org",
            timestamp = 1714500000000L,
            contentHash = 42
        )
        val second = SyncEventProcessor.buildDeterministicEventKey(
            roomId = "!roomB:example.org",
            senderId = "@alice:example.org",
            timestamp = 1714500000000L,
            contentHash = 42
        )

        assertNotEquals(first, second)
    }
}
