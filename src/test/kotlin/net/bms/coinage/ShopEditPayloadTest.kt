package net.bms.coinage

import io.netty.buffer.Unpooled
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ShopEditPayloadTest {
    @Test
    fun `edit requests retain full offer numbers expressions and explicit deletion`() {
        val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
        try {
            val original = ShopEditPayloads.Edit(100, Int.MAX_VALUE, Int.MAX_VALUE,
                "platinum + 2 * iron + 5", true, false)
            ShopEditPayloads.EDIT_CODEC.encode(buffer, original)
            assertEquals(original, ShopEditPayloads.EDIT_CODEC.decode(buffer))
            val removal = original.copy(expression = "", removing = true)
            ShopEditPayloads.EDIT_CODEC.encode(buffer, removal)
            assertEquals(removal, ShopEditPayloads.EDIT_CODEC.decode(buffer))
            assertEquals(0, buffer.readableBytes())
        } finally {
            buffer.release()
        }
    }

    @Test
    fun `oversized expressions are rejected before reaching the expression parser`() {
        val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
        try {
            buffer.writeVarInt(1)
            buffer.writeVarInt(1)
            buffer.writeVarInt(1)
            buffer.writeUtf("1".repeat(ShopEditPayloads.MAX_EXPRESSION + 1))
            buffer.writeBoolean(false)
            buffer.writeBoolean(false)
            assertThrows(RuntimeException::class.java) { ShopEditPayloads.EDIT_CODEC.decode(buffer) }
        } finally {
            buffer.release()
        }
    }

    @Test
    fun `replies retain the request correlation and result code`() {
        val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
        try {
            val reply = ShopEditPayloads.Result(100, Int.MAX_VALUE, 3)
            ShopEditPayloads.RESULT_CODEC.encode(buffer, reply)
            assertEquals(reply, ShopEditPayloads.RESULT_CODEC.decode(buffer))
            assertEquals(0, buffer.readableBytes())
        } finally {
            buffer.release()
        }
    }
}
