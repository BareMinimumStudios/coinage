package net.bms.coinage

import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent

@Mod(MOD_ID)
class CoinageNeoForge(modBus: net.neoforged.bus.api.IEventBus) {
    init {
        modBus.addListener { event: net.neoforged.neoforge.registries.RegisterEvent ->
            event.register(net.minecraft.core.registries.Registries.ITEM) { registry ->
                CoinageItems.entries.forEach { (name, item) ->
                    registry.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item)
                }
            }
        }
        modBus.addListener { event: net.neoforged.neoforge.registries.RegisterEvent ->
            event.register(net.minecraft.core.registries.Registries.BLOCK) { registry ->
                CoinageBlocks.piggyBanks.forEach { (name, block) ->
                    registry.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, name), block)
                }
            }
            event.register(net.minecraft.core.registries.Registries.ITEM) { registry ->
                CoinageBlocks.items.forEach { (name, item) ->
                    registry.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, name), item)
                }
            }
            event.register(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE) { registry ->
                CoinageBlocks.piggyType = net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    ::PiggyBankEntity, *CoinageBlocks.piggyBanks.values.toTypedArray()
                ).build(null)
                registry.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "piggy_bank"), CoinageBlocks.piggyType)
            }
        }
        val commands = EconomyCommands(EconomyConfig.register())
        NeoForge.EVENT_BUS.addListener { event: RegisterCommandsEvent -> commands.register(event.dispatcher) }
    }
}
