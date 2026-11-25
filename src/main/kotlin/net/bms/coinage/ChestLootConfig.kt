package net.bms.coinage

import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt

@Translatable.Name("Chest currency")
class ChestLootConfig : ConfigSection() {
    var enabled = true
    var desertMinimum = ValidatedInt(300, 0..1_000_000_000)
    var desertMaximum = ValidatedInt(1200, 0..1_000_000_000)
    var dungeonMinimum = ValidatedInt(500, 0..1_000_000_000)
    var dungeonMaximum = ValidatedInt(2000, 0..1_000_000_000)
    var structureMinimum = ValidatedInt(1500, 0..1_000_000_000)
    var structureMaximum = ValidatedInt(4000, 0..1_000_000_000)
    var libraryMinimum = ValidatedInt(2000, 0..1_000_000_000)
    var libraryMaximum = ValidatedInt(6000, 0..1_000_000_000)

    @Translatable.Desc("Extra loot table IDs and base bronze values. Bags vary from half to one-and-a-half times the base at 75% chance. Reload data after editing.")
    var extraTables: Map<String, Int> = emptyMap()
}
