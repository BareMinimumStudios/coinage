package net.bms.coinage

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PurseMigrationTest {
    private val player = UUID(0, 1)

    @Test
    fun spentLegacyMoneyCannotBeImportedAgain() {
        val ledger = PurseLedger()
        assertTrue(ledger.importLegacy(player, 500))
        ledger.setBalance(player, 0)
        assertFalse(ledger.importLegacy(player, 500))
        assertEquals(0L, ledger.balance(player))
    }

    @Test
    fun explicitZeroBalanceTakesPrecedenceOverLegacyMoney() {
        val ledger = PurseLedger()
        ledger.setBalance(player, 0)
        assertFalse(ledger.importLegacy(player, 900))
        assertEquals(0L, ledger.balance(player))
    }

    @Test
    fun existingCoinageBalanceIsNeverAddedToLegacyBalance() {
        val ledger = PurseLedger()
        ledger.setBalance(player, 37)
        assertFalse(ledger.importLegacy(player, Long.MAX_VALUE))
        assertEquals(37L, ledger.balance(player))
    }

    @Test
    fun invalidImportDoesNotPreventAValidImportLater() {
        val ledger = PurseLedger()
        assertThrows(IllegalArgumentException::class.java) { ledger.importLegacy(player, -1) }
        assertFalse(ledger.hasLegacyImport(player))
        assertTrue(ledger.importLegacy(player, Long.MAX_VALUE))
        assertEquals(Long.MAX_VALUE, ledger.balance(player))
    }
}
