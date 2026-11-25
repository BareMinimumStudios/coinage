package net.bms.coinage

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.stream.Stream

class PurseLedgerTest {
    private val sender = UUID(0, 1)
    private val recipient = UUID(0, 2)

    @Test
    fun `the sum of player balances can exceed a long`() {
        val ledger = PurseLedger()
        assertEquals("0", ledger.totalBalance().toString())
        ledger.setBalance(sender, Long.MAX_VALUE)
        ledger.setBalance(recipient, Long.MAX_VALUE)
        assertEquals("18446744073709551614", ledger.totalBalance().toString())
    }

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
        val saved = ledger.save(CompoundTag(), HolderLookup.Provider.create(Stream.empty()))
        val restored = PurseLedger.load(saved)
        assertEquals(30L, restored.balance(sender))
        assertEquals(80L, restored.balance(recipient))
        assertEquals(listOf(recipient), restored.richest(1).map { it.key })
    }
}
