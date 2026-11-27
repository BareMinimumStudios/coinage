# Coinage

**Project summary**

Physical coins, a personal purse, player shops, and configurable trading for Minecraft.

**Project description**

Find coins in loot, earn them from eligible mobs and merchants, and spend them at shops run by other players. Coinage brings a physical currency and a configurable economy to Minecraft.

## Earn, carry, and spend

- **Four coin tiers.** Bronze, silver, gold, and platinum each stack to 100. By default, 100 coins of one tier trade up to one coin of the next. Servers can change those exchange rates.
- **A purse for every player.** Press **B**, click the inventory purse, or run `/coinage purse` to open it. Withdraw physical coins or bundle an amount into a money bag. Send money directly with `/coinage pay` when the server enables player payments.
- **Optional automatic collection.** Run `/coinage autocollect toggle` to send picked-up coins straight to your purse. The preference is saved per player and starts off.
- **Piggy banks and money bags.** Quick-stack physical currency into a piggy bank, find money bags in loot, and configure how much purse money drops on death. Compatible versions of You're in Grave Danger and GraveStone Mod can capture the dropped bag.

## Shops and merchants

- **Shops and pawn shops.** Set your own offers and prices. Shops sell from stock, and pawn shops buy goods with funds you supply. Server operators can use inexhaustible variants.
- **Villager and wandering trader offers.** Use Coinage's trade profiles, add them alongside native trades, or leave trades to Minecraft and other mods. Coinage can pay eligible trades from the purse.
- **Optional shared prices.** Shop prices react to what players buy and sell across the world. The market resets to base prices after a configurable interval. The default is two hours of game time at 20 TPS.
- **Optional Easy NPC integration.** Open named Coinage shops from NPC actions, or use shared market prices in eligible trades in Easy NPC's built-in trading screen.

Server owners configure the economy through Fzzy Config. Datapacks can define merchant offers, extend the mob currency drop tag, and set money bag values in loot.

## Installation

Fabric and NeoForge builds target **Minecraft 1.21.1** (Java 21) and **Minecraft 26.3** (Java 25). Install the matching build and dependencies on the server and on each modded client.

- **Fabric** · Fabric API, Fabric Language Kotlin, and Fzzy Config
- **NeoForge** · Kotlin for Forge and Fzzy Config

See the [Coinage README](https://github.com/BareMinimumStudios/coinage#readme) for commands, configuration examples, and datapack setup. Report issues in the [GitHub issue tracker](https://github.com/BareMinimumStudios/coinage/issues).

By pokesmells and Bare Minimum Studios. Coinage uses the [Bare Minimum License](https://github.com/BareMinimumStudios/coinage/blob/main/LICENSE). See the [upstream attribution](https://github.com/BareMinimumStudios/coinage/blob/main/NOTICE.md) for material adapted from Numismatic Overhaul.
