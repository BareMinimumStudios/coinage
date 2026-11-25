package net.bms.coinage

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

internal object PurseWithdrawPayloads {
    const val MAX_EXPRESSION = 256
    val WITHDRAW = CustomPacketPayload.Type<Withdraw>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "purse_withdraw"))
    val RESULT = CustomPacketPayload.Type<Result>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "purse_withdraw_result"))
    val QUICK = CustomPacketPayload.Type<Quick>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "purse_quick"))
    val SELECTED = CustomPacketPayload.Type<Selected>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "purse_selected_v2"))
    var receive: (Result) -> Unit = {}
    val WITHDRAW_CODEC = object : StreamCodec<RegistryFriendlyByteBuf, Withdraw> {
        override fun decode(buffer: RegistryFriendlyByteBuf) = Withdraw(buffer.readVarInt(), buffer.readVarInt(),
            buffer.readUtf(MAX_EXPRESSION), buffer.readBoolean())
        override fun encode(buffer: RegistryFriendlyByteBuf, value: Withdraw) {
            buffer.writeVarInt(value.containerId)
            buffer.writeVarInt(value.requestId)
            buffer.writeUtf(value.expression, MAX_EXPRESSION)
            buffer.writeBoolean(value.bag)
        }
    }
    val SELECTED_CODEC = object : StreamCodec<RegistryFriendlyByteBuf, Selected> {
        override fun decode(buffer: RegistryFriendlyByteBuf): Selected {
            val container = buffer.readVarInt()
            val request = buffer.readVarInt()
            val counts = List(CoinTier.entries.size) { buffer.readUnsignedByte().toInt().also { require(it <= 100) } }
            return Selected(container, request, counts, buffer.readBoolean())
        }
        override fun encode(buffer: RegistryFriendlyByteBuf, value: Selected) {
            require(value.counts.size == CoinTier.entries.size && value.counts.all { it in 0..100 })
            buffer.writeVarInt(value.containerId)
            buffer.writeVarInt(value.requestId)
            value.counts.forEach(buffer::writeByte)
            buffer.writeBoolean(value.bag)
        }
    }
    val QUICK_CODEC = object : StreamCodec<RegistryFriendlyByteBuf, Quick> {
        override fun decode(buffer: RegistryFriendlyByteBuf) = Quick(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt())
        override fun encode(buffer: RegistryFriendlyByteBuf, value: Quick) {
            buffer.writeVarInt(value.containerId)
            buffer.writeVarInt(value.requestId)
            buffer.writeVarInt(value.action)
        }
    }
    val RESULT_CODEC = object : StreamCodec<RegistryFriendlyByteBuf, Result> {
        override fun decode(buffer: RegistryFriendlyByteBuf) = Result(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt())
        override fun encode(buffer: RegistryFriendlyByteBuf, value: Result) {
            buffer.writeVarInt(value.containerId)
            buffer.writeVarInt(value.requestId)
            buffer.writeVarInt(value.code)
        }
    }
    data class Withdraw(val containerId: Int, val requestId: Int, val expression: String,
        val bag: Boolean) : CustomPacketPayload { override fun type() = WITHDRAW }
    data class Selected(val containerId: Int, val requestId: Int, val counts: List<Int>,
        val bag: Boolean) : CustomPacketPayload { override fun type() = SELECTED }
    data class Quick(val containerId: Int, val requestId: Int, val action: Int) : CustomPacketPayload { override fun type() = QUICK }
    data class Result(val containerId: Int, val requestId: Int, val code: Int) : CustomPacketPayload { override fun type() = RESULT }
}
