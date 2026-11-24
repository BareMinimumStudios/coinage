package net.bms.coinage

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

internal class EconomyCommands(private val config: EconomyConfig) {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(literal("coinage")
            .then(literal("balance").executes(::showOwnBalance)
                .then(argument("player", EntityArgument.player())
                    .requires { it.hasPermission(2) }
                    .executes(::showTargetBalance)
                    .then(literal("set").then(argument("coins", StringArgumentType.greedyString())
                        .executes(::setBalance)))
                    .then(literal("add").then(argument("coins", StringArgumentType.greedyString())
                        .executes { adjustBalance(it, false) }))
                    .then(literal("subtract").then(argument("coins", StringArgumentType.greedyString())
                        .executes { adjustBalance(it, true) }))))
            .then(literal("top").requires { it.hasPermission(2) }
                .executes { showRichest(it, 10) }
                .then(argument("limit", IntegerArgumentType.integer(1, 100))
                    .executes { showRichest(it, IntegerArgumentType.getInteger(it, "limit")) }))
            .then(literal("pay").requires { it.entity is ServerPlayer && config.playerPayments }
                .then(argument("player", EntityArgument.player())
                    .then(argument("coins", StringArgumentType.greedyString()).executes(::pay)))))
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
                ?: Component.literal(server.profileCache?.get(id)?.orElse(null)?.name ?: id.toString())
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

    private fun coinAmount(context: CommandContext<CommandSourceStack>, minimum: Long): Long {
        return CoinAmount.parse(StringArgumentType.getString(context, "coins"), minimum)
            ?: throw invalidAmount.create()
    }

    private companion object {
        val invalidAmount = SimpleCommandExceptionType(Component.translatable("command.coinage.amount.invalid"))
        val paymentFailed = SimpleCommandExceptionType(Component.translatable("command.coinage.payment.failed"))
    }
}
