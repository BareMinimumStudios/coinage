package net.bms.coinage

import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.LootTableLoadEvent
import net.neoforged.neoforge.event.TagsUpdatedEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.registries.RegisterEvent

@Mod(MOD_ID)
class CoinageNeoForge(modBus: IEventBus) {
    init {
        modBus.addListener { event: RegisterEvent ->
            event.register(Registries.ITEM) { registry ->
                CoinageItems.entries.forEach { (name, item) ->
                    registry.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item)
                }
            }
        }
        modBus.addListener { event: RegisterEvent ->
            event.register(Registries.BLOCK) { registry ->
                CoinageBlocks.piggyBanks.forEach { (name, block) ->
                    registry.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), block)
                }
            }
            event.register(Registries.ITEM) { registry ->
                CoinageBlocks.items.forEach { (name, item) ->
                    registry.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item)
                }
            }
            event.register(Registries.BLOCK_ENTITY_TYPE) { registry ->
                CoinageBlocks.piggyType = BlockEntityType.Builder.of(
                    ::PiggyBankEntity, *CoinageBlocks.piggyBanks.values.toTypedArray()
                ).build(null)
                registry.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "piggy_bank"), CoinageBlocks.piggyType)
            }
        }
        modBus.addListener { event: RegisterEvent ->
            event.register(Registries.BLOCK) { registry ->
                CoinageBlocks.shops.forEach { (name, block) ->
                    registry.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), block)
                }
            }
            event.register(Registries.ITEM) { registry ->
                CoinageBlocks.shopItems.forEach { (name, item) ->
                    registry.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item)
                }
            }
            event.register(Registries.BLOCK_ENTITY_TYPE) { registry ->
                CoinageBlocks.shopType = BlockEntityType.Builder.of(
                    ::ShopEntity, *CoinageBlocks.shops.values.toTypedArray()
                ).build(null)
                registry.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "shop"), CoinageBlocks.shopType)
            }
        }
        modBus.addListener { event: RegisterEvent ->
            event.register(Registries.CREATIVE_MODE_TAB) { registry ->
                registry.register(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"),
                    CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.coinage"))
                        .icon { ItemStack(CoinageItems.platinum) }
                        .displayItems { _, output -> CoinageCreative.fill(output) }
                        .build()
                )
            }
        }
        modBus.addListener { event: RegisterEvent ->
            event.register(Registries.LOOT_FUNCTION_TYPE) { registry ->
                registry.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "set_money"), SetMoneyFunction.TYPE)
            }
        }
        modBus.addListener { event: RegisterEvent ->
            event.register(Registries.RECIPE_SERIALIZER) { registry ->
                registry.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "piggy_bank_coloring"), PiggyBankColoringRecipe.SERIALIZER)
            }
        }
        modBus.addListener { event: RegisterEvent ->
            event.register(Registries.SOUND_EVENT) { registry ->
                registry.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "piggy_bank_break"), PiggyBankEffects.breakSound)
            }
        }
        val config = EconomyConfig.register()
        NeoForge.EVENT_BUS.addListener { event: LootTableLoadEvent ->
            if (!event.isCanceled) ChestCurrencyLoot.pools(event.name.toString(), config.chestLoot).forEach { (name, pool) ->
                if (event.table.getPool(name) == null) event.table.addPool(pool.name(name).build())
            }
        }
        val commands = EconomyCommands(config)
        val events = EconomyEvents(config)
        NeoForge.EVENT_BUS.addListener { event: LivingDeathEvent ->
            if (!event.isCanceled) events.onDeath(event.entity, event.source)
        }
        NeoForge.EVENT_BUS.addListener { event: TagsUpdatedEvent -> events.clearDropCache() }
        NeoForge.EVENT_BUS.addListener { event: RegisterCommandsEvent -> commands.register(event.dispatcher) }
    }
}
