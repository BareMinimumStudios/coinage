package net.bms.coinage

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.trading.MerchantOffers
import org.slf4j.LoggerFactory

object TradeCatalog {
    private val logger = LoggerFactory.getLogger("Coinage/Trades")
    private var loadedFor: MinecraftServer? = null
    @Volatile private var profiles: Map<String, Map<Int, List<TradeDefinition>>> = emptyMap()
    var rejectedDefinitions = 0
        private set
    val definitionCount: Int get() = profiles.values.sumOf { levels -> levels.values.sumOf { it.size } }
    val profileCount: Int get() = profiles.size

    fun reload(server: MinecraftServer) {
        val next = linkedMapOf<String, MutableMap<Int, MutableList<TradeDefinition>>>()
        var rejected = 0
        val resources = server.resourceManager.listResources("villager_trades") { it.path.endsWith(".json") }
        for ((id, resource) in resources.entries.sortedWith(compareBy({ it.key.namespace != MOD_ID }, { it.key.toString() }))) {
            try {
                val root = resource.openAsReader().use { JsonParser.parseReader(it).asJsonObject }
                val profession = TradePlatform.id(root.get("profession").asString).toString()
                if (root.get("replace")?.asBoolean == true) next.remove(profession)
                val levels = next.getOrPut(profession) { linkedMapOf() }
                for ((name, entries) in root.getAsJsonObject("trades").entrySet()) {
                    val level = LEVELS[name] ?: name.toIntOrNull()?.takeIf { it in 1..5 }
                        ?: error("Unknown merchant level '$name'")
                    val list = levels.getOrPut(level) { mutableListOf() }
                    for ((index, entry) in entries.asJsonArray.withIndex()) {
                        try {
                            list.add(TradeDefinition.parse(entry.asJsonObject, server, "$id/$name/$index"))
                        } catch (exception: RuntimeException) {
                            rejected++
                            logger.warn("Rejected trade {}/{}/{}: {}", id, name, index, exception.message)
                        }
                    }
                }
            } catch (exception: Exception) {
                rejected++
                logger.warn("Could not load merchant profile {}: {}", id, exception.message)
            }
        }
        profiles = next.mapValues { (_, levels) -> levels.mapValues { it.value.toList() } }
        rejectedDefinitions = rejected
        loadedFor = server
        logger.info("Loaded {} trade definitions for {} merchant profiles ({} rejected)", definitionCount, profileCount, rejected)
    }

    private fun config(entity: LivingEntity): MerchantTradeConfig? {
        val config = CurrencyValues.configuration?.merchantTrades ?: return null
        if (config.mode == TradeMode.DISABLED) return null
        if (TradePlatform.profession(entity) in config.excludedProfessions) return null
        val level = entity.level() as? ServerLevel ?: return null
        if (loadedFor !== level.server) reload(level.server)
        return config
    }

    @JvmStatic
    fun replace(entity: LivingEntity, offers: MerchantOffers): Boolean {
        val config = config(entity) ?: return false
        if (config.mode != TradeMode.REPLACE || !profiles.containsKey(TradePlatform.profession(entity))) return false
        append(entity, offers, config)
        return true
    }

    @JvmStatic
    fun afterNative(entity: LivingEntity, offers: MerchantOffers) {
        val config = config(entity) ?: return
        if (config.mode == TradeMode.APPEND) append(entity, offers, config)
        remap(entity, offers)
    }

    private fun append(entity: LivingEntity, offers: MerchantOffers, config: MerchantTradeConfig) {
        val levels = profiles[TradePlatform.profession(entity)] ?: return
        val level = TradePlatform.level(entity)
        val selections = if (level == 0) listOf(1 to 5, 2 to 1, 3 to 2) else listOf(level to config.offersPerLevel.get())
        for ((tier, count) in selections) {
            val candidates = levels[tier]?.toMutableList() ?: continue
            var added = 0
            while (added < count && candidates.isNotEmpty()) {
                val definition = candidates.removeAt(entity.random.nextInt(candidates.size))
                try {
                    val offer = definition.create(entity) ?: continue
                    offers.add(offer)
                    added++
                } catch (exception: RuntimeException) {
                    logger.warn("Could not generate trade {}: {}", definition.source, exception.message)
                }
            }
        }
    }

    @JvmStatic
    fun remap(entity: LivingEntity, offers: MerchantOffers) {
        val config = config(entity) ?: return
        if (!config.convertEmeralds) return
        for (index in offers.indices) offers[index] = TradePlatform.remap(offers[index], config.bronzePerEmerald.get())
    }

    private val LEVELS = mapOf("novice" to 1, "apprentice" to 2, "journeyman" to 3, "expert" to 4, "master" to 5,
        "common" to 1, "rare" to 2, "uncommon" to 2, "buying" to 3)
}
