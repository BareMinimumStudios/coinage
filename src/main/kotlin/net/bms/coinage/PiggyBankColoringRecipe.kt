package net.bms.coinage

import java.util.Locale
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.DyeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

class PiggyBankColoringRecipe(category: CraftingBookCategory) : CustomRecipe(category) {
    override fun matches(input: CraftingInput, level: Level): Boolean = ingredients(input) != null

    private fun ingredients(input: CraftingInput): Pair<ItemStack, DyeItem>? {
        var bank: ItemStack? = null
        var dye: DyeItem? = null
        for (slot in 0 until input.size()) {
            val stack = input.getItem(slot)
            if (stack.isEmpty) continue
            when {
                Block.byItem(stack.item) is PiggyBankBlock && bank == null -> bank = stack
                stack.item is DyeItem && dye == null -> dye = stack.item as DyeItem
                else -> return null
            }
        }
        return bank?.let { stack -> dye?.let { stack to it } }
    }

    private fun recolor(input: CraftingInput): ItemStack {
        val (bank, dye) = ingredients(input) ?: return ItemStack.EMPTY
        val target = CoinageBlocks.items.getValue(dye.dyeColor.name.lowercase(Locale.ROOT) + "_piggy_bank")
        return bank.transmuteCopy(target, 1)
    }

    override fun assemble(input: CraftingInput, registries: HolderLookup.Provider): ItemStack = recolor(input)
    override fun canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2
    override fun getSerializer(): RecipeSerializer<*> = SERIALIZER

    companion object {
        val SERIALIZER = SimpleCraftingRecipeSerializer(::PiggyBankColoringRecipe)
    }
}
