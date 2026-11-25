package net.bms.coinage

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleMenuProvider

internal object RemoteShops {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(literal("coinage").then(literal("shop").then(literal("open")
            .then(argument("shop", StringArgumentType.word())
                .suggests { _, builder ->
                    CurrencyValues.configuration?.market?.remoteShops?.keys
                        ?.filter { it.startsWith(builder.remainingLowerCase) }?.sorted()?.forEach { builder.suggest(it) }
                    builder.buildFuture()
                }
                .executes { context ->
                    val player = context.source.playerOrException
                    val name = StringArgumentType.getString(context, "shop")
                    val shop = resolve(player, name)
                    if (!player.isAlive || player.isSpectator || shop == null) {
                        context.source.sendFailure(Component.translatable("command.coinage.shop_unavailable", name))
                        0
                    } else {
                        if (player.openMenu(SimpleMenuProvider({ id, inventory, _ ->
                            ShopMenu(id, inventory, shop, name)
                        }, shop.displayName)).isPresent) 1 else 0
                    }
                }))))
    }

    fun resolve(player: ServerPlayer, name: String): ShopEntity? {
        val parts = CurrencyValues.configuration?.market?.remoteShops?.get(name)?.trim()
            ?.split(Regex("""\s+""")) ?: return null
        if (parts.size != 4) return null
        val dimension = ResourceLocation.tryParse(parts[0]) ?: return null
        val level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension)) ?: return null
        val pos = BlockPos(parts[1].toIntOrNull() ?: return null, parts[2].toIntOrNull() ?: return null,
            parts[3].toIntOrNull() ?: return null)
        if (!level.hasChunkAt(pos)) return null
        return level.getBlockEntity(pos) as? ShopEntity
    }
}
