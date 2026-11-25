package net.bms.coinage

import java.util.IdentityHashMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.gamerules.GameRules

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

                    selector.startsWith("€") && selector.substring(1).equals(entity.type.category.name, ignoreCase = true) -> 0

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
        val level = entity.level() as? ServerLevel ?: return


        if (entity is ServerPlayer) {
            if (level.gameRules.get(GameRules.KEEP_INVENTORY)) return

            val ledger = PurseLedger.get(entity.level().server)

            val balance = ledger.balance(entity.uuid)

            val percent = config.deathLossPercent.coerceIn(0, 100)

            val loss = balance / 100 * percent + balance % 100 * percent / 100

            if (loss == 0L) return

            entity.spawnAtLocation(level, MoneyBagItem.create(loss)) ?: return

            ledger.setBalance(entity.uuid, balance - loss)

            return
        }

        val killer = source.entity as? ServerPlayer ?: entity.killCredit as? ServerPlayer ?: return

        if (!level.gameRules.get(GameRules.MOB_DROPS)) return

        val base = baseDrop(entity)

        if (base == 0L) return

        val looting = if (config.lootingBonusPercent > 0) {
            val enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING)
            EnchantmentHelper.getEnchantmentLevel(enchantment, killer)
        } else 0
        val amount = MobCurrencyDrops.amount(
            base, config.mobDropVariancePercent, level.random.nextDouble(), entity.maxHealth.toDouble(),
            config.scaleMobDropsOnHealth, config.mobHealthDivisor, looting, config.lootingBonusPercent
        )

        if (amount > 0) entity.spawnAtLocation(level, MoneyBagItem.create(amount))
    }
}
