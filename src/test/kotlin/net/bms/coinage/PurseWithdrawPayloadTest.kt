package net.bms.coinage

import io.netty.buffer.Unpooled
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PurseWithdrawPayloadTest {
    @Test
    fun `withdrawals preserve expressions payout type and correlation`() {
        val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
        try {
            for (bag in listOf(false, true)) {
                val request = PurseWithdrawPayloads.Withdraw(100, Int.MAX_VALUE, "2 * iron + 5 * bronze", bag)
                PurseWithdrawPayloads.WITHDRAW_CODEC.encode(buffer, request)
                assertEquals(request, PurseWithdrawPayloads.WITHDRAW_CODEC.decode(buffer))
            }
            val reply = PurseWithdrawPayloads.Result(100, Int.MAX_VALUE, 5)
            PurseWithdrawPayloads.RESULT_CODEC.encode(buffer, reply)
            assertEquals(reply, PurseWithdrawPayloads.RESULT_CODEC.decode(buffer))
            assertEquals(0, buffer.readableBytes())
        } finally {
            buffer.release()
        }
    }

    @Test
    fun `oversized withdrawal expressions are rejected during decoding`() {
        val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
        try {
            buffer.writeVarInt(1)
            buffer.writeVarInt(1)
            buffer.writeUtf("1".repeat(PurseWithdrawPayloads.MAX_EXPRESSION + 1))
            buffer.writeBoolean(true)
            assertThrows(RuntimeException::class.java) { PurseWithdrawPayloads.WITHDRAW_CODEC.decode(buffer) }
        } finally {
            buffer.release()
        }
    }
}
