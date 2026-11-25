package net.bms.coinage

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
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

class SetMoneyFunction private constructor(
    conditions: List<LootItemCondition>,
    private val value: Either<NumberProvider, IntRange>
) :
    LootItemConditionalFunction(conditions) {
    override fun getType(): LootItemFunctionType<SetMoneyFunction> = TYPE
    override fun getReferencedContextParams(): Set<LootContextParam<*>> = value.map({ it.referencedContextParams }, { emptySet() })

    override fun run(stack: ItemStack, context: LootContext): ItemStack {
        if (stack.item !is MoneyBagItem) return stack
        val amount = value.map(
            { it.getInt(context) },
            { context.random.nextIntBetweenInclusive(it.first, it.last) }
        ).toLong()
        if (amount <= 0) return ItemStack.EMPTY
        val data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
        data.putLong("coinage_value", amount)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data))
        return stack
    }

    companion object {
        private val bound = Codec.intRange(0, 1_500_000_000)
        private val rangeCodec = RecordCodecBuilder.mapCodec<IntRange> { instance ->
            instance.group(
                bound.fieldOf("min").forGetter<IntRange> { it.first },
                bound.fieldOf("max").forGetter<IntRange> { it.last }
            ).apply(instance) { min, max -> min..max }
        }.validate { range ->
            if (range.isEmpty()) DataResult.error { "Chest currency range is reversed" } else DataResult.success(range)
        }

        val CODEC: MapCodec<SetMoneyFunction> = RecordCodecBuilder.mapCodec { instance ->
            commonFields(instance).and(Codec.mapEither(
                NumberProviders.CODEC.fieldOf("value"), rangeCodec.codec().fieldOf("range")
            ).forGetter<SetMoneyFunction> { it.value })
                .apply(instance, ::SetMoneyFunction)
        }
        val TYPE = LootItemFunctionType(CODEC)

        fun between(minimum: Int, maximum: Int): Builder<*> = simpleBuilder { conditions ->
            SetMoneyFunction(conditions, Either.right(minimum..maximum))
        }
    }
}
