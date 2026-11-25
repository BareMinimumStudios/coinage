package net.bms.coinage

import java.util.Locale
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.tags.TagKey
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes

internal object ExplorerMaps {
    data class Presentation(val icon: Holder<MapDecorationType>, val name: Component)
    private data class Rule(val tag: TagKey<Structure>, val icon: Holder<MapDecorationType>, val name: String)

    private fun rule(tag: String, icon: Holder<MapDecorationType>, name: String) =
        Rule(TagKey.create(Registries.STRUCTURE, TradePlatform.id(tag)), icon, "filled_map.$name")

    private val rules = listOf(
        rule("minecraft:on_treasure_maps", MapDecorationTypes.RED_X, "buried_treasure"),
        rule("minecraft:on_ocean_explorer_maps", MapDecorationTypes.OCEAN_MONUMENT, "monument"),
        rule("minecraft:on_ocean_monument_maps", MapDecorationTypes.OCEAN_MONUMENT, "monument"),
        rule("minecraft:on_woodland_explorer_maps", MapDecorationTypes.WOODLAND_MANSION, "mansion"),
        rule("minecraft:on_woodland_mansion_maps", MapDecorationTypes.WOODLAND_MANSION, "mansion"),
        rule("c:on_woodland_explorer_maps", MapDecorationTypes.WOODLAND_MANSION, "mansion"),
        rule("minecraft:on_desert_village_maps", MapDecorationTypes.DESERT_VILLAGE, "village_desert"),
        rule("minecraft:on_savanna_village_maps", MapDecorationTypes.SAVANNA_VILLAGE, "village_savanna"),
        rule("minecraft:on_plains_village_maps", MapDecorationTypes.PLAINS_VILLAGE, "village_plains"),
        rule("minecraft:on_taiga_village_maps", MapDecorationTypes.TAIGA_VILLAGE, "village_taiga"),
        rule("minecraft:on_snowy_village_maps", MapDecorationTypes.SNOWY_VILLAGE, "village_snowy"),
        rule("minecraft:on_jungle_explorer_maps", MapDecorationTypes.JUNGLE_TEMPLE, "explorer_jungle"),
        rule("minecraft:on_jungle_pyramid_maps", MapDecorationTypes.JUNGLE_TEMPLE, "explorer_jungle"),
        rule("minecraft:on_swamp_explorer_maps", MapDecorationTypes.SWAMP_HUT, "explorer_swamp"),
        rule("minecraft:on_swamp_hut_maps", MapDecorationTypes.SWAMP_HUT, "explorer_swamp"),
        rule("minecraft:on_trial_chambers_maps", MapDecorationTypes.TRIAL_CHAMBERS, "trial_chambers"),
        rule("minecraft:on_buried_trial_chambers_maps", MapDecorationTypes.TRIAL_CHAMBERS, "trial_chambers")
    )

    fun presentation(structure: Holder<Structure>): Presentation {
        val rule = rules.lastOrNull { structure.`is`(it.tag) }
        if (rule != null) return Presentation(rule.icon, Component.translatable(rule.name))
        val path = TradePlatform.structurePath(structure)
        val readable = path.substringAfterLast('/').split('_', '-').filter { it.isNotEmpty() }
            .joinToString(" ") { it.replaceFirstChar { character -> character.titlecase(Locale.ROOT) } }
        return Presentation(MapDecorationTypes.TARGET_X,
            Component.translatableWithFallback("filled_map.$path", "$readable Explorer Map"))
    }
}
