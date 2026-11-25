package net.bms.coinage

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

internal object PursePayloads {
    const val PROTOCOL = 2
    private fun <T : CustomPacketPayload> type(path: String) =
        CustomPacketPayload.Type<T>(ResourceLocation.fromNamespaceAndPath(MOD_ID, path))
    val HELLO = type<Hello>("purse_hello")
    val OPEN = type<Open>("purse_open")
    val SHOW = type<Show>("purse_show")
    var show: () -> Unit = {}
    val SNAPSHOT = type<Snapshot>("purse_snapshot_v2")

    val HELLO_CODEC = codec({ Hello(it.readVarInt()) }, { buffer, value -> buffer.writeVarInt(value.protocol) })
    val OPEN_CODEC = codec({ Open() }, { _, _ -> })
    val SHOW_CODEC = codec({ Show() }, { _, _ -> })
    val SNAPSHOT_CODEC = codec({ buffer ->
        Snapshot(buffer.readVarLong(), ExchangeRates(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()))
    }, { buffer, value ->
        buffer.writeVarLong(value.balance)
        buffer.writeVarInt(value.rates.bronzePerSilver)
        buffer.writeVarInt(value.rates.silverPerGold)
        buffer.writeVarInt(value.rates.goldPerPlatinum)
    })

    private fun <T : Any> codec(read: (RegistryFriendlyByteBuf) -> T, write: (RegistryFriendlyByteBuf, T) -> Unit) =
        object : StreamCodec<RegistryFriendlyByteBuf, T> {
            override fun decode(buffer: RegistryFriendlyByteBuf): T = read(buffer)
            override fun encode(buffer: RegistryFriendlyByteBuf, value: T) = write(buffer, value)
        }

    data class Hello(val protocol: Int = PROTOCOL) : CustomPacketPayload { override fun type() = HELLO }
    class Show : CustomPacketPayload { override fun type() = SHOW }
    class Open : CustomPacketPayload { override fun type() = OPEN }
    data class Snapshot(val balance: Long, val rates: ExchangeRates) : CustomPacketPayload { override fun type() = SNAPSHOT }
}
