package net.bms.coinage

import net.minecraft.world.item.ItemStack

import net.minecraft.network.chat.Component

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BlockEntityType

object CoinageFabric : ModInitializer {
    override fun onInitialize() {
        CoinageItems.entries.forEach { (name, item) ->
            Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(MOD_ID, name), item
            )
        }
        CoinageBlocks.piggyBanks.forEach { (name, block) ->
            val id = Identifier.fromNamespaceAndPath(MOD_ID, name)
            Registry.register(BuiltInRegistries.BLOCK, id, block)
            Registry.register(BuiltInRegistries.ITEM, id, CoinageBlocks.items.getValue(name))
        }
        CoinageBlocks.piggyType = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "piggy_bank"),
            BlockEntityType(::PiggyBankEntity, CoinageBlocks.piggyBanks.values.toSet())
        )
        CoinageBlocks.shops.forEach { (name, block) ->
            val id = Identifier.fromNamespaceAndPath(MOD_ID, name)
            Registry.register(BuiltInRegistries.BLOCK, id, block)
            Registry.register(BuiltInRegistries.ITEM, id, CoinageBlocks.shopItems.getValue(name))
        }
        CoinageBlocks.shopType = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "shop"),
            BlockEntityType(::ShopEntity, CoinageBlocks.shops.values.toSet())
        )
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MOD_ID, "main"),
            FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.coinage"))
                .icon { ItemStack(CoinageItems.platinum) }
                .displayItems { _, output -> CoinageCreative.fill(output) }
                .build()
        )
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "set_money"), SetMoneyFunction.CODEC)
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
            Identifier.fromNamespaceAndPath(MOD_ID, "piggy_bank_coloring"), PiggyBankColoringRecipe.SERIALIZER)
        Registry.register(BuiltInRegistries.SOUND_EVENT,
            Identifier.fromNamespaceAndPath(MOD_ID, "piggy_bank_break"), PiggyBankEffects.breakSound)
        val config = EconomyConfig.register()
        CoinageFabricNetwork.register()
        ShopFabricAutomation.register()
        LootTableEvents.MODIFY.register { key, table, _, _ ->
            ChestCurrencyLoot.pools(key.identifier().toString(), config.chestLoot).forEach { (_, pool) -> table.withPool(pool) }
        }
        val commands = EconomyCommands(config)
        val events = EconomyEvents(config)
        ServerLivingEntityEvents.AFTER_DEATH.register(events::onDeath)
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            TradeCatalog.reload(server)
        }
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { server, _, success ->
            if (success) {
                events.clearDropCache()
                TradeCatalog.reload(server)
            }
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> commands.register(dispatcher) }
    }
}
