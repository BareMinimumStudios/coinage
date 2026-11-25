package net.bms.coinage

import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition

internal object ChestCurrencyLoot {
    fun pools(table: String, config: ChestLootConfig): List<Pair<String, LootPool.Builder>> {
        if (!config.enabled) return emptyList()
        val rule = when (table) {
            "minecraft:chests/desert_pyramid" -> Rule(config.desertMinimum.get(), config.desertMaximum.get(), .45f, false)
            "minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft" ->
                Rule(config.dungeonMinimum.get(), config.dungeonMaximum.get(), .75f, true)
            "minecraft:chests/bastion_treasure", "minecraft:chests/stronghold_corridor",
            "minecraft:chests/pillager_outpost", "minecraft:chests/buried_treasure" ->
                Rule(config.structureMinimum.get(), config.structureMaximum.get(), .75f, true)
            "minecraft:chests/stronghold_library" -> Rule(config.libraryMinimum.get(), config.libraryMaximum.get(), .85f, true)
            else -> config.extraTables[table]?.coerceIn(0, 1_000_000_000)?.takeIf { it > 0 }?.let {
                Rule(it / 2, it + it / 2, .75f, false)
            } ?: return emptyList()
        }
        val pools = mutableListOf<Pair<String, LootPool.Builder>>()
        val minimum = minOf(rule.minimum, rule.maximum)
        val maximum = maxOf(rule.minimum, rule.maximum)
        if (maximum > 0) pools.add("coinage_currency" to LootPool.lootPool()
            .add(LootItem.lootTableItem(CoinageItems.moneyBag).apply(SetMoneyFunction.between(minimum, maximum)))
            .`when`(LootItemRandomChanceCondition.randomChance(rule.chance)))
        if (rule.rareGold) pools.add("coinage_rare_gold" to LootPool.lootPool()
            .add(LootItem.lootTableItem(CoinageItems.gold))
            .`when`(LootItemRandomChanceCondition.randomChance(.01f)))
        return pools
    }

    private data class Rule(val minimum: Int, val maximum: Int, val chance: Float, val rareGold: Boolean)
}
