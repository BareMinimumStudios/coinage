package net.bms.coinage

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
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
        val commands = EconomyCommands(EconomyConfig.register())
        NeoForge.EVENT_BUS.addListener { event: RegisterCommandsEvent -> commands.register(event.dispatcher) }
    }
}
