package net.bms.coinage

import net.minecraft.nbt.NbtOps
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class PurseLedgerTest {
    private val sender = UUID(0, 1)
    private val recipient = UUID(0, 2)

    @Test
    fun `invalid transfers preserve both balances`() {
        val ledger = PurseLedger()
        ledger.setBalance(sender, 12)
        ledger.setBalance(recipient, Long.MAX_VALUE)
        assertFalse(ledger.transfer(sender, recipient, 1))
        assertFalse(ledger.transfer(sender, recipient, 13))
        assertEquals(12L, ledger.balance(sender))
        assertEquals(Long.MAX_VALUE, ledger.balance(recipient))
    }

    @Test
    fun `transfers persist and the ranking is bounded`() {
        val ledger = PurseLedger()
        ledger.setBalance(sender, 100)
        ledger.setBalance(recipient, 10)
        assertTrue(ledger.transfer(sender, recipient, 70))
        val saved = PurseLedger.codec.encodeStart(NbtOps.INSTANCE, ledger).orThrow
        val restored = PurseLedger.codec.parse(NbtOps.INSTANCE, saved).orThrow
        assertEquals(30L, restored.balance(sender))
        assertEquals(80L, restored.balance(recipient))
        assertEquals(listOf(recipient), restored.richest(1).map { it.key })
    }
}
