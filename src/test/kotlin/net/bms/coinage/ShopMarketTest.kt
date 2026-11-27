package net.bms.coinage

import com.mojang.serialization.JsonOps
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShopMarketTest {
    @Test
    fun `supply reduces its own price and raises prices of other goods`() {
        val market = ShopMarket()
        market.refresh(0, 60) {}
        val expression = "100 + pressure + spillover"
        market.record("minecraft:wheat", -20)
        assertEquals(80L, market.quote("minecraft:wheat", 100, expression))
        assertEquals(102L, market.quote("minecraft:carrot", 100, expression))
        market.record("minecraft:wheat", 10)
        assertEquals(90L, market.quote("minecraft:wheat", 100, expression))
        assertEquals(101L, market.quote("minecraft:carrot", 100, expression))
        market.record("minecraft:wheat", 30)
        market.record("minecraft:carrot", -40)
        assertEquals(124L, market.quote("minecraft:wheat", 100, expression))
        assertEquals(60L, market.quote("minecraft:carrot", 100, expression))
        assertEquals(100L, market.quote("minecraft:wheat", 100, "bad expression"))
    }

    @Test
    fun `prices are bounded and saved pressure expires after a reset`() {
        val market = ShopMarket()
        val interval = MarketConfig().resetIntervalSeconds.get()
        assertEquals(7_200, interval)
        market.refresh(0, interval) {}
        market.record("minecraft:wheat", -100)
        val encoded = ShopMarket.codec.encodeStart(JsonOps.INSTANCE, market).result().orElseThrow()
        val loaded = ShopMarket.codec.parse(JsonOps.INSTANCE, encoded).result().orElseThrow()
        assertEquals(50L, loaded.quote("minecraft:wheat", 100, "100 + pressure * 10"))
        assertEquals(Long.MAX_VALUE, loaded.quote("minecraft:carrot", Long.MAX_VALUE, "150"))
        assertEquals(105L, loaded.quote("minecraft:carrot", 100, "100 + spillover"))
        val expression = "100 + pressure"
        loaded.refresh(143_999, 60) { error("Reset fired before the saved deadline") }
        assertEquals(50L, loaded.quote("minecraft:wheat", 100, expression))
        loaded.refresh(144_000, 60) {}
        assertEquals(100L, loaded.quote("minecraft:wheat", 100, expression))

        loaded.record("minecraft:wheat", -20)
        loaded.refresh(145_199, 60) { error("Custom interval reset early") }
        assertEquals(80L, loaded.quote("minecraft:wheat", 100, expression))
        assertEquals(102L, loaded.quote("minecraft:carrot", 100, "100 + spillover"))
        loaded.refresh(145_200, 60) {}
        assertEquals(100L, loaded.quote("minecraft:wheat", 100, expression))
    }
}
