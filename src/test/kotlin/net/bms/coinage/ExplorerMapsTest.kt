package net.bms.coinage

import net.minecraft.SharedConstants
import net.minecraft.core.Holder
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.core.HolderOwner
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.Bootstrap
import net.minecraft.tags.TagKey
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ExplorerMapsTest {
    private fun structure(path: String, vararg tags: String): Holder<Structure> {
        val key = ResourceKey.create(Registries.STRUCTURE, TradePlatform.id(path))
        val holder = Holder.Reference.createStandAlone(object : HolderOwner<Structure> {}, key)
        val binding = Holder.Reference::class.java.getDeclaredMethod("bindTags", Collection::class.java)
        binding.isAccessible = true
        binding.invoke(holder, tags.map { TagKey.create(Registries.STRUCTURE, TradePlatform.id(it)) })
        return holder
    }

    @Test
    fun `ocean maps retain their monument marker and localized title`() {
        val result = ExplorerMaps.presentation(structure("minecraft:monument", "minecraft:on_ocean_explorer_maps"))
        assertSame(MapDecorationTypes.OCEAN_MONUMENT, result.icon)
        assertEquals("filled_map.monument", (result.name.contents as TranslatableContents).key)
    }

    @Test
    fun `shared woodland tags work for modded structures`() {
        val result = ExplorerMaps.presentation(structure("example:great_manor", "c:on_woodland_explorer_maps"))
        assertSame(MapDecorationTypes.WOODLAND_MANSION, result.icon)
        assertEquals("filled_map.mansion", (result.name.contents as TranslatableContents).key)
    }

    @Test
    fun `specific explorer categories take precedence over a general treasure tag`() {
        val result = ExplorerMaps.presentation(structure("example:underground_vault",
            "minecraft:on_treasure_maps", "minecraft:on_trial_chambers_maps"))
        assertSame(MapDecorationTypes.TRIAL_CHAMBERS, result.icon)
        assertEquals("filled_map.trial_chambers", (result.name.contents as TranslatableContents).key)
    }

    @Test
    fun `modern renamed map tags retain their native presentation`() {
        val cases = listOf(
            Triple("on_ocean_monument_maps", MapDecorationTypes.OCEAN_MONUMENT, "filled_map.monument"),
            Triple("on_woodland_mansion_maps", MapDecorationTypes.WOODLAND_MANSION, "filled_map.mansion"),
            Triple("on_jungle_pyramid_maps", MapDecorationTypes.JUNGLE_TEMPLE, "filled_map.explorer_jungle"),
            Triple("on_swamp_hut_maps", MapDecorationTypes.SWAMP_HUT, "filled_map.explorer_swamp"),
            Triple("on_buried_trial_chambers_maps", MapDecorationTypes.TRIAL_CHAMBERS, "filled_map.trial_chambers")
        )
        for ((tag, icon, name) in cases) {
            val result = ExplorerMaps.presentation(structure("example:destination", "minecraft:$tag"))
            assertSame(icon, result.icon)
            assertEquals(name, (result.name.contents as TranslatableContents).key)
        }
    }

    @Test
    fun `unknown modded structures have a readable name without requiring a translation pack`() {
        val result = ExplorerMaps.presentation(structure("example:ruins/ancient_tower"))
        assertSame(MapDecorationTypes.TARGET_X, result.icon)
        assertEquals("Ancient Tower Explorer Map", result.name.string)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }
}
