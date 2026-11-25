package net.bms.coinage

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.world.item.trading.Merchant

internal object MerchantCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(literal("coinage").then(literal("trades").requires { it.hasPermission(2) }
            .then(literal("status").executes { context ->
                context.source.sendSuccess({ Component.translatable("command.coinage.trades.status",
                    TradeCatalog.definitionCount, TradeCatalog.profileCount, TradeCatalog.rejectedDefinitions) }, false)
                TradeCatalog.definitionCount
            })
            .then(literal("reload").executes { context ->
                TradeCatalog.reload(context.source.server)
                context.source.sendSuccess({ Component.translatable("command.coinage.trades.reloaded") }, true)
                TradeCatalog.definitionCount
            })
            .then(literal("inspect").then(argument("merchant", EntityArgument.entity()).executes { context ->
                val entity = EntityArgument.getEntity(context, "merchant")
                val merchant = entity as? Merchant
                if (merchant == null) {
                    context.source.sendFailure(Component.translatable("command.coinage.trades.not_merchant"))
                    0
                } else {
                    val offers = merchant.offers
                    context.source.sendSuccess({ Component.translatable("command.coinage.trades.inspected", entity.displayName, offers.size) }, false)
                    offers.size
                }
            }))))
    }
}
