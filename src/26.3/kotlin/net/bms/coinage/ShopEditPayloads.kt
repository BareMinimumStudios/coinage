package net.bms.coinage

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

internal object ShopEditPayloads {
    const val MAX_EXPRESSION = 256
    val EDIT = CustomPacketPayload.Type<Edit>(Identifier.fromNamespaceAndPath(MOD_ID, "shop_edit"))
    val RESULT = CustomPacketPayload.Type<Result>(Identifier.fromNamespaceAndPath(MOD_ID, "shop_edit_result"))
    var receive: (Result) -> Unit = {}
    val EDIT_CODEC = object : StreamCodec<RegistryFriendlyByteBuf, Edit> {
        override fun decode(buffer: RegistryFriendlyByteBuf) = Edit(buffer.readVarInt(), buffer.readVarInt(),
            buffer.readVarInt(), buffer.readUtf(MAX_EXPRESSION), buffer.readBoolean(), buffer.readBoolean())
        override fun encode(buffer: RegistryFriendlyByteBuf, value: Edit) {
            buffer.writeVarInt(value.containerId)
            buffer.writeVarInt(value.requestId)
            buffer.writeVarInt(value.offerNumber)
            buffer.writeUtf(value.expression, MAX_EXPRESSION)
            buffer.writeBoolean(value.useExisting)
            buffer.writeBoolean(value.removing)
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
    data class Edit(val containerId: Int, val requestId: Int, val offerNumber: Int,
        val expression: String, val useExisting: Boolean, val removing: Boolean = false) : CustomPacketPayload { override fun type() = EDIT }
    data class Result(val containerId: Int, val requestId: Int, val code: Int) : CustomPacketPayload { override fun type() = RESULT }
}
