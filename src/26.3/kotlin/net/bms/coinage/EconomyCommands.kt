package net.bms.coinage

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import java.util.Locale
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permission
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.world.phys.BlockHitResult

internal class EconomyCommands(private val config: EconomyConfig) {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        MerchantCommands.register(dispatcher)
        dispatcher.register(literal("coinage").then(literal("shop").then(literal("automation")
            .executes { shopAutomation(it, null) }
            .then(argument("enabled", BoolArgumentType.bool())
                .executes { shopAutomation(it, BoolArgumentType.getBool(it, "enabled")) }))))
        dispatcher.register(literal("coinage").executes { PurseMenu.open(it.source.playerOrException); 1 }
            .then(literal("balance").executes(::showOwnBalance)
                .then(argument("player", EntityArgument.player())
                    .requires { it.permissions().hasPermission(Permission.HasCommandLevel(PermissionLevel.byId(2))) }
                    .executes(::showTargetBalance)
                    .then(literal("get").executes(::showTargetBalance))
                    .then(literal("set").then(argument("coins", StringArgumentType.greedyString())
                        .executes(::setBalance)))
                    .then(literal("add").then(argument("coins", StringArgumentType.greedyString())
                        .executes { adjustBalance(it, false) }))
                    .then(literal("subtract").then(argument("coins", StringArgumentType.greedyString())
                        .executes { adjustBalance(it, true) }))))
            .then(literal("purse").executes {
                PurseMenu.open(it.source.playerOrException)
                1
            })
            .then(literal("deposit").executes(::deposit)
                .then(literal("all").executes(::deposit)))
            .then(literal("rates").executes(::showRates))
            .then(literal("shop")
                .then(literal("offer").then(argument("slot", IntegerArgumentType.integer(1))
                    .then(argument("coins", StringArgumentType.greedyString()).executes(::configureShop))))
                .then(literal("fund").then(argument("coins", StringArgumentType.greedyString()).executes(::fundShop))))
            .then(literal("bag")
                .then(argument("coins", StringArgumentType.greedyString()).executes(::makeBag)))
            .then(literal("withdraw")
                .then(argument("coins", StringArgumentType.greedyString()).executes(::withdraw)))
            .then(literal("top").requires { it.permissions().hasPermission(Permission.HasCommandLevel(PermissionLevel.byId(2))) }
                .executes { showRichest(it, 10) }
                .then(argument("limit", IntegerArgumentType.integer(1, 100))
                    .executes { showRichest(it, IntegerArgumentType.getInteger(it, "limit")) }))
            .then(literal("pay").requires { it.entity is ServerPlayer && config.playerPayments }
                .then(argument("player", EntityArgument.player())
                    .then(argument("coins", StringArgumentType.greedyString()).executes(::pay)))))
    }

    private fun showRates(context: CommandContext<CommandSourceStack>): Int {
        val rates = CurrencyValues.current()
        val ratios = listOf(rates.bronzePerSilver, rates.silverPerGold, rates.goldPerPlatinum)
        for (index in ratios.indices) {
            val lower = Component.translatable("item.coinage." + CoinTier.entries[index].name.lowercase(Locale.ROOT) + "_coin")
            val higher = Component.translatable("item.coinage." + CoinTier.entries[index + 1].name.lowercase(Locale.ROOT) + "_coin")
            context.source.sendSuccess({ Component.translatable("command.coinage.rate", ratios[index], lower, higher) }, false)
        }
        return 1
    }
    private fun showOwnBalance(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.playerOrException
        val balance = PurseLedger.get(source.server).balance(player.uuid)
        source.sendSuccess({ Component.translatable("command.coinage.balance", balance) }, false)
        return balance.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun showTargetBalance(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = EntityArgument.getPlayer(context, "player")
        val balance = PurseLedger.get(source.server).balance(player.uuid)
        source.sendSuccess({ Component.translatable("command.coinage.balance.other", player.displayName, balance) }, false)
        return balance.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun setBalance(context: CommandContext<CommandSourceStack>): Int {
        val player = EntityArgument.getPlayer(context, "player")
        PurseLedger.get(context.source.server).setBalance(player.uuid, coinAmount(context, 0))
        return showTargetBalance(context)
    }

    private fun adjustBalance(context: CommandContext<CommandSourceStack>, subtract: Boolean): Int {
        val player = EntityArgument.getPlayer(context, "player")
        val ledger = PurseLedger.get(context.source.server)
        val amount = coinAmount(context, 0)
        val value = try {
            if (subtract) Math.subtractExact(ledger.balance(player.uuid), amount)
            else Math.addExact(ledger.balance(player.uuid), amount)
        } catch (_: ArithmeticException) {
            throw invalidAmount.create()
        }
        if (value < 0) throw invalidAmount.create()
        ledger.setBalance(player.uuid, value)
        return showTargetBalance(context)
    }

    private fun showRichest(context: CommandContext<CommandSourceStack>, limit: Int): Int {
        val source = context.source
        val server = source.server
        val records = PurseLedger.get(server).richest(limit)
        if (records.isEmpty()) {
            source.sendSuccess({ Component.translatable("command.coinage.top.empty") }, false)
            return 0
        }
        records.forEachIndexed { index, (id, balance) ->
            val name = server.playerList.getPlayer(id)?.displayName
                ?: Component.literal(id.toString())
            source.sendSuccess({ Component.translatable("command.coinage.top.entry", index + 1, name, balance) }, false)
        }
        return records.size
    }

    private fun pay(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val sender = source.playerOrException
        val recipient = EntityArgument.getPlayer(context, "player")
        val amount = coinAmount(context, 1)
        if (!config.playerPayments || sender.uuid == recipient.uuid ||
            !PurseLedger.get(source.server).transfer(sender.uuid, recipient.uuid, amount)
        ) {
            throw paymentFailed.create()
        }
        source.sendSuccess({ Component.translatable("command.coinage.payment.sent", amount, recipient.displayName) }, false)
        recipient.sendSystemMessage(Component.translatable("command.coinage.payment.received", amount, sender.displayName))
        return 1
    }

    private fun targetShop(context: CommandContext<CommandSourceStack>): ShopEntity {
        val player = context.source.playerOrException
        val hit = player.pick(6.0, 0f, false) as? BlockHitResult
            ?: throw paymentFailed.create()
        return player.level().getBlockEntity(hit.blockPos) as? ShopEntity ?: throw paymentFailed.create()
    }

    private fun shopAutomation(context: CommandContext<CommandSourceStack>, enabled: Boolean?): Int {
        val shop = targetShop(context)
        if (!shop.setAutomation(context.source.playerOrException, enabled ?: !shop.automationEnabled)) throw paymentFailed.create()
        context.source.sendSuccess({ Component.translatable("command.coinage.shop.automation",
            Component.translatable(if (shop.automationEnabled) "options.on" else "options.off")) }, false)
        return 1
    }

    private fun configureShop(context: CommandContext<CommandSourceStack>): Int {
        val shop = targetShop(context)
        if (!shop.configure(context.source.playerOrException,
                IntegerArgumentType.getInteger(context, "slot") - 1, coinAmount(context, 0))) {
            throw paymentFailed.create()
        }
        context.source.sendSuccess({ Component.translatable("command.coinage.shop.configured") }, false)
        return 1
    }

    private fun fundShop(context: CommandContext<CommandSourceStack>): Int {
        if (!targetShop(context).fund(context.source.playerOrException, coinAmount(context, 1))) throw paymentFailed.create()
        context.source.sendSuccess({ Component.translatable("command.coinage.shop.funded") }, false)
        return 1
    }

    private fun deposit(context: CommandContext<CommandSourceStack>): Int {
        val amount = PurseTransactions.deposit(context.source.playerOrException) ?: throw paymentFailed.create()
        context.source.sendSuccess({ Component.translatable("command.coinage.deposited", amount) }, false)
        return 1
    }

    private fun makeBag(context: CommandContext<CommandSourceStack>): Int {
        if (!PurseTransactions.withdraw(context.source.playerOrException, coinAmount(context, 1), bag = true)) {
            throw paymentFailed.create()
        }
        return 1
    }

    private fun withdraw(context: CommandContext<CommandSourceStack>): Int {
        if (!PurseTransactions.withdraw(context.source.playerOrException, coinAmount(context, 1))) {
            throw paymentFailed.create()
        }
        return 1
    }

    private fun coinAmount(context: CommandContext<CommandSourceStack>, minimum: Long): Long {
        return CoinAmount.parse(StringArgumentType.getString(context, "coins"), minimum)
            ?: throw invalidAmount.create()
    }

    private companion object {
        val invalidAmount = SimpleCommandExceptionType(Component.translatable("command.coinage.amount.invalid"))
        val paymentFailed = SimpleCommandExceptionType(Component.translatable("command.coinage.payment.failed"))
    }
}
