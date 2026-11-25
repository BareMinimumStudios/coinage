package net.bms.coinage

import io.netty.buffer.Unpooled
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PursePayloadTest {
    @AfterEach
    fun clearClientState() { ClientPurseState.reset() }

    @Test
    fun snapshotPreservesMaximumBalancesAndCustomRatesWithinSeventeenBytes() {
        val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
        try {
            val original = PursePayloads.Snapshot(Long.MAX_VALUE, ExchangeRates(10_000, 10_000, 10_000, 10_000))
            PursePayloads.SNAPSHOT_CODEC.encode(buffer, original)
            assertTrue(buffer.readableBytes() <= 17)
            assertEquals(original, PursePayloads.SNAPSHOT_CODEC.decode(buffer))
            assertEquals(0, buffer.readableBytes())
        } finally {
            buffer.release()
        }
    }

    @Test
    fun disconnectClearsBothBalanceAndServerRates() {
        ClientPurseState.accept(PursePayloads.Snapshot(123, ExchangeRates(2, 3, 4, 5)))
        assertTrue(ClientPurseState.ready)
        ClientPurseState.reset()
        assertFalse(ClientPurseState.ready)
        assertEquals(0L, ClientPurseState.balance)
        assertEquals(ExchangeRates(), ClientPurseState.rates)
    }

    @Test
    fun invalidBalanceCannotReplaceTheLastValidSnapshot() {
        val valid = PursePayloads.Snapshot(123, ExchangeRates(2, 3, 4, 5))
        ClientPurseState.accept(valid)
        ClientPurseState.accept(PursePayloads.Snapshot(-1, ExchangeRates()))
        assertEquals(valid.balance, ClientPurseState.balance)
        assertEquals(valid.rates, ClientPurseState.rates)
    }
}
