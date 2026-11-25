package net.bms.coinage

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.Optional
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.Validatable
import net.minecraft.world.level.storage.loot.ValidationContext
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders
import net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator

class SetMoneyFunction(condition: Optional<Holder<LootItemCondition>>, private val value: Holder<ContextIntProvider>) :
    LootItemConditionalFunction(condition) {
    override fun codec(): MapCodec<SetMoneyFunction> = CODEC

    override fun validate(context: ValidationContext) {
        super.validate(context)
        Validatable.validateHolder(context, "value", value)
    }

    override fun run(stack: ItemStack, context: LootContext): ItemStack {
        if (stack.item !is MoneyBagItem) return stack
        val amount = value.value().getInt(context).toLong()
        if (amount <= 0) return ItemStack.EMPTY
        val data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
        data.putLong("coinage_value", amount)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data))
        return stack
    }

    companion object {
        val CODEC: MapCodec<SetMoneyFunction> = RecordCodecBuilder.mapCodec { instance ->
            commonFields(instance).and(ContextIntProviders.CODEC.fieldOf("value").forGetter<SetMoneyFunction> { it.value })
                .apply(instance, ::SetMoneyFunction)
        }

        fun between(minimum: Int, maximum: Int): Builder<*> = simpleBuilder { condition ->
            SetMoneyFunction(condition, Holder.direct(UniformGenerator(
                ContextIntProviders.exactly(minimum), ContextIntProviders.exactly(maximum)
            )))
        }
    }
}
