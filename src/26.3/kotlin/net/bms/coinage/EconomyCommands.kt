package net.bms.coinage

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import java.math.BigInteger
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
        dispatcher.register(literal("coinage").then(literal("autocollect")
            .executes { autoCollect(it, null, toggle = false) }
            .then(literal("toggle").executes { autoCollect(it, null, toggle = true) })
            .then(argument("enabled", BoolArgumentType.bool())
                .executes { autoCollect(it, BoolArgumentType.getBool(it, "enabled"), toggle = false) })))
        dispatcher.register(literal("coinage").then(literal("shop").then(literal("automation")
            .executes { shopAutomation(it, null) }
            .then(argument("enabled", BoolArgumentType.bool())
                .executes { shopAutomation(it, BoolArgumentType.getBool(it, "enabled")) }))))
        dispatcher.register(literal("coinage").executes { PurseMenu.open(it.source.playerOrException); 1 }
            .then(literal("balance").executes(::showOwnBalance)
                .then(argument("player", EntityArgument.players())
                    .requires { it.permissions().hasPermission(Permission.HasCommandLevel(PermissionLevel.byId(2))) }
                    .executes(::showTargetBalance)
                    .then(literal("get").executes(::showTargetBalance))
                    .then(literal("set").then(CoinAmountArgument.argument()
                        .executes(::setBalance)))
                    .then(literal("add").then(CoinAmountArgument.argument()
                        .executes { adjustBalance(it, false) }))
                    .then(literal("subtract").then(CoinAmountArgument.argument()
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
                    .then(CoinAmountArgument.argument().executes(::configureShop))))
                .then(literal("fund").then(CoinAmountArgument.argument().executes(::fundShop))))
            .then(literal("bag")
                .then(CoinAmountArgument.argument().executes(::makeBag)))
            .then(literal("withdraw")
                .then(CoinAmountArgument.argument().executes(::withdraw)))
            .then(literal("serverworth").requires { it.permissions().hasPermission(Permission.HasCommandLevel(PermissionLevel.byId(2))) }.executes(::showServerWorth))
            .then(literal("top").requires { it.permissions().hasPermission(Permission.HasCommandLevel(PermissionLevel.byId(2))) }
                .executes { showRichest(it, 10) }
                .then(argument("limit", IntegerArgumentType.integer(1, 100))
                    .executes { showRichest(it, IntegerArgumentType.getInteger(it, "limit")) }))
            .then(literal("pay").requires { it.entity is ServerPlayer && config.playerPayments }
                .then(argument("player", EntityArgument.player())
                    .then(CoinAmountArgument.argument().executes(::pay)))))
    }

    private fun autoCollect(context: CommandContext<CommandSourceStack>, enabled: Boolean?, toggle: Boolean): Int {
        val player = context.source.playerOrException
        val ledger = PurseLedger.get(player.level().server)
        val current = ledger.autoCollect(player.uuid)
        val result = enabled ?: if (toggle) !current else current
        ledger.setAutoCollect(player.uuid, result)
        val message = if (result) "command.coinage.autocollect.on" else "command.coinage.autocollect.off"
        context.source.sendSuccess({ Component.translatable(message) }, false)
        return 1
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
        val ledger = PurseLedger.get(source.server)
        var total = BigInteger.ZERO
        for (player in EntityArgument.getPlayers(context, "player")) {
            val balance = ledger.balance(player.uuid)
            reportBalance(source, player, balance)
            total += BigInteger.valueOf(balance)
        }
        return total.min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt()
    }

    private fun setBalance(context: CommandContext<CommandSourceStack>): Int =
        updateTargetBalances(context) { _, amount -> amount }

    private fun adjustBalance(context: CommandContext<CommandSourceStack>, subtract: Boolean): Int =
        updateTargetBalances(context) { balance, amount ->
            if (subtract) Math.subtractExact(balance, amount) else Math.addExact(balance, amount)
        }

    private fun updateTargetBalances(context: CommandContext<CommandSourceStack>, update: (Long, Long) -> Long): Int {
        val source = context.source
        val ledger = PurseLedger.get(source.server)
        val amount = coinAmount(context, 0)
        val changes = EntityArgument.getPlayers(context, "player").map { player ->
            val value = try {
                update(ledger.balance(player.uuid), amount)
            } catch (_: ArithmeticException) {
                throw invalidAmount.create()
            }
            if (value < 0) throw invalidAmount.create()
            player to value
        }
        for ((player, balance) in changes) {
            ledger.setBalance(player.uuid, balance)
            reportBalance(source, player, balance)
        }
        return changes.last().second.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun reportBalance(source: CommandSourceStack, player: ServerPlayer, balance: Long) {
        source.sendSuccess({ Component.translatable("command.coinage.balance.other", player.displayName, balance) }, false)
    }

    private fun showServerWorth(context: CommandContext<CommandSourceStack>): Int {
        val worth = PurseLedger.get(context.source.server).totalBalance()
        context.source.sendSuccess({ Component.translatable("command.coinage.server_worth", worth.toString()) }, false)
        return worth.min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt()
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
        return CoinAmount.parse(StringArgumentType.getString(context, CoinAmountArgument.NAME), minimum)
            ?: throw invalidAmount.create()
    }

    private companion object {
        val invalidAmount = SimpleCommandExceptionType(Component.translatable("command.coinage.amount.invalid"))
        val paymentFailed = SimpleCommandExceptionType(Component.translatable("command.coinage.payment.failed"))
    }
}
