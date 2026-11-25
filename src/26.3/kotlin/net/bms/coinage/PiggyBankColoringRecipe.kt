package net.bms.coinage

import com.mojang.serialization.MapCodec
import java.util.Locale
import net.minecraft.core.component.DataComponents
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block

class PiggyBankColoringRecipe : CustomRecipe() {
    override fun matches(input: CraftingInput, level: Level): Boolean = ingredients(input) != null

    private fun ingredients(input: CraftingInput): Pair<ItemStack, DyeColor>? {
        var bank: ItemStack? = null
        var dye: DyeColor? = null
        for (slot in 0 until input.size()) {
            val stack = input.getItem(slot)
            if (stack.isEmpty) continue
            when {
                Block.byItem(stack.item) is PiggyBankBlock && bank == null -> bank = stack
                stack.get(DataComponents.DYE) != null && dye == null -> dye = stack.get(DataComponents.DYE)
                else -> return null
            }
        }
        return bank?.let { stack -> dye?.let { stack to it } }
    }

    private fun recolor(input: CraftingInput): ItemStack {
        val (bank, dye) = ingredients(input) ?: return ItemStack.EMPTY
        val target = CoinageBlocks.items.getValue(dye.name.lowercase(Locale.ROOT) + "_piggy_bank")
        return bank.transmuteCopy(target, 1)
    }

    override fun assemble(input: CraftingInput): ItemStack = recolor(input)
    override fun getSerializer(): RecipeSerializer<PiggyBankColoringRecipe> = SERIALIZER

    companion object {
        val SERIALIZER = RecipeSerializer(MapCodec.unit(::PiggyBankColoringRecipe), StreamCodec.unit(PiggyBankColoringRecipe()))
    }
}
