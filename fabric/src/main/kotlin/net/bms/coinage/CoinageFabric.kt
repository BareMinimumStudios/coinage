package net.bms.coinage

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
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
        CoinageBlocks.shops.forEach { (name, block) ->
            val id = ResourceLocation.fromNamespaceAndPath(MOD_ID, name)
            Registry.register(BuiltInRegistries.BLOCK, id, block)
            Registry.register(BuiltInRegistries.ITEM, id, CoinageBlocks.shopItems.getValue(name))
        }
        CoinageBlocks.shopType = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "shop"),
            BlockEntityType.Builder.of(::ShopEntity, *CoinageBlocks.shops.values.toTypedArray()).build(null)
        )
        val config = EconomyConfig.register()
        val commands = EconomyCommands(config)
        val events = EconomyEvents(config)
        ServerLivingEntityEvents.AFTER_DEATH.register(events::onDeath)
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, success -> if (success) events.clearDropCache() }
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> commands.register(dispatcher) }
    }
}
