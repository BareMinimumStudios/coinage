package net.bms.coinage

import java.util.IdentityHashMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.GameRules

internal class EconomyEvents(private val config: EconomyConfig) {
    private val dropValues = IdentityHashMap<EntityType<*>, Long>()

    private var dropConfig: Map<String, Int> = emptyMap()

    fun clearDropCache() {
        dropValues.clear()
    }

    private fun baseDrop(entity: LivingEntity): Long {
        val current = config.mobDrops

        if (dropConfig !== current) {
            dropConfig = current

            clearDropCache()
        }

        return dropValues.getOrPut(entity.type) {
            val id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)

            var result = 0L

            var priority = -1

            for ((selector, amount) in current) {
                val rank = when {
                    selector == id.toString() -> 3

                    selector.startsWith("#") && entity.type.builtInRegistryHolder().tags().anyMatch {
                        it.location().toString() == selector.substring(1)
                    } -> 2

                    selector == "@" + id.namespace -> 1

                    selector.startsWith("€") && selector.substring(1)
                        .equals(entity.type.category.name, ignoreCase = true) -> 0

                    else -> -1
                }

                if (rank > priority) {
                    result = amount.coerceAtLeast(0).toLong()

                    priority = rank
                }
            }

            result
        }
    }

    fun onDeath(entity: LivingEntity, source: DamageSource) {
        val level = entity.level()

        if (level.isClientSide) return

        if (entity is ServerPlayer) {
            if (level.gameRules.getBoolean(GameRules.RULE_KEEPINVENTORY)) return

            val ledger = PurseLedger.get(entity.server)

            val balance = ledger.balance(entity.uuid)

            val percent = config.deathLossPercent.coerceIn(0, 100)

            val loss = balance / 100 * percent + balance % 100 * percent / 100

            if (loss == 0L) return

            entity.spawnAtLocation(MoneyBagItem.create(loss)) ?: return

            ledger.setBalance(entity.uuid, balance - loss)

            return
        }

        if (source.entity !is ServerPlayer) return

        if (!level.gameRules.getBoolean(GameRules.RULE_DOMOBLOOT)) return

        val base = baseDrop(entity)

        if (base == 0L) return

        val variance = config.mobDropVariancePercent.coerceIn(0, 100) / 100.0

        val amount =
            (base.toDouble() * (1 - variance + level.random.nextDouble() * variance * 2)).toLong().coerceAtLeast(0)

        if (amount > 0) entity.spawnAtLocation(MoneyBagItem.create(amount))
    }
}
