package net.bms.coinage

import net.minecraft.world.item.CreativeModeTab

internal object CoinageCreative {
    fun fill(output: CreativeModeTab.Output) {
        CoinageItems.entries.values.forEach { output.accept(it) }
        CoinageBlocks.items.values.forEach { output.accept(it) }
        CoinageBlocks.shopItems.values.forEach { output.accept(it) }
    }
}
