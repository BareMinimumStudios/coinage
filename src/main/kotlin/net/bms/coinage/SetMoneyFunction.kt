package net.bms.coinage

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType
import net.minecraft.world.level.storage.loot.parameters.LootContextParam
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator

class SetMoneyFunction(conditions: List<LootItemCondition>, private val value: NumberProvider) :
    LootItemConditionalFunction(conditions) {
    override fun getType(): LootItemFunctionType<SetMoneyFunction> = TYPE
    override fun getReferencedContextParams(): Set<LootContextParam<*>> = value.referencedContextParams

    override fun run(stack: ItemStack, context: LootContext): ItemStack {
        if (stack.item !is MoneyBagItem) return stack
        val amount = value.getInt(context).toLong()
        if (amount <= 0) return ItemStack.EMPTY
        val data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
        data.putLong("coinage_value", amount)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data))
        return stack
    }

    companion object {
        val CODEC: MapCodec<SetMoneyFunction> = RecordCodecBuilder.mapCodec { instance ->
            commonFields(instance).and(NumberProviders.CODEC.fieldOf("value").forGetter<SetMoneyFunction> { it.value })
                .apply(instance, ::SetMoneyFunction)
        }
        val TYPE = LootItemFunctionType(CODEC)

        fun between(minimum: Int, maximum: Int): Builder<*> = simpleBuilder { conditions ->
            SetMoneyFunction(conditions, UniformGenerator.between(minimum.toFloat(), maximum.toFloat()))
        }
    }
}
