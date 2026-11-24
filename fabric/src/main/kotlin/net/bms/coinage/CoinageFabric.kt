package net.bms.coinage

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

object CoinageFabric : ModInitializer {
    override fun onInitialize() {
        CoinageItems.entries.forEach { (name, item) ->
            net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item
            )
        }
        CoinageBlocks.piggyBanks.forEach { (name, block) ->
            val id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, name)
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK, id, block)
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id, CoinageBlocks.items.getValue(name))
        }
        CoinageBlocks.piggyType = net.minecraft.core.Registry.register(
            net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "piggy_bank"),
            net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(::PiggyBankEntity, *CoinageBlocks.piggyBanks.values.toTypedArray()).build(null)
        )
        val commands = EconomyCommands(EconomyConfig.register())
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> commands.register(dispatcher) }
    }
}
