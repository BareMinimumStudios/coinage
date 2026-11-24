package net.bms.coinage

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType

object CoinageFabric : ModInitializer {
    override fun onInitialize() {
        CoinageItems.entries.forEach { (name, item) ->
            Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item
            )
        }
        CoinageBlocks.piggyBanks.forEach { (name, block) ->
            val id = ResourceLocation.fromNamespaceAndPath(MOD_ID, name)
            Registry.register(BuiltInRegistries.BLOCK, id, block)
            Registry.register(BuiltInRegistries.ITEM, id, CoinageBlocks.items.getValue(name))
        }
        CoinageBlocks.piggyType = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "piggy_bank"),
            BlockEntityType.Builder.of(::PiggyBankEntity, *CoinageBlocks.piggyBanks.values.toTypedArray()).build(null)
        )
        val commands = EconomyCommands(EconomyConfig.register())
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> commands.register(dispatcher) }
    }
}
