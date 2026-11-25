# Coinage

Coinage adds physical coins, a personal purse, piggy banks, player shops, pawn shops, and currency trades for villagers and wandering traders. By pokesmells and Bare Minimum Studios.

## Install

Pick the Coinage JAR for your exact Minecraft version and loader. Install Coinage and its dependencies on the server and on each modded client.

| Minecraft | Java | Fabric dependencies | NeoForge dependencies |
| --- | --- | --- | --- |
| 1.21.1 | 21 | Fabric API, Fabric Language Kotlin, Fzzy Config | Kotlin for Forge, Fzzy Config |
| 26.3 | 25 | Fabric API, Fabric Language Kotlin, Fzzy Config | Kotlin for Forge, Fzzy Config |

The [target files](targets/) record the versions used to build and test each release. Crunch is bundled.

## Use the purse

Press **B**, click the purse icon in your inventory, or run `/coinage purse`. Use the arrows to choose denominations. Hold Shift to adjust by ten. The extraction button withdraws the selected coins, and Ctrl-clicking it puts the selected value into one money bag. Shift-click the purse icon to deposit all currency from your inventory. Shift + Ctrl on the extraction button withdraws the full balance as coins if they fit.

Coins stack to 100. The default rates are 100 bronze to 1 silver, 100 silver to 1 gold, and 100 gold to 1 platinum. Servers can change the three ratios in Fzzy Config. Enter plain amounts in bronze or use expressions such as `2 * silver + 5 * bronze`.

| Command | Use |
| --- | --- |
| `/coinage balance` | Check your purse balance. |
| `/coinage rates` | Show the server's exchange rates. |
| `/coinage deposit` | Deposit currency from your inventory. |
| `/coinage withdraw <amount>` | Withdraw physical coins. |
| `/coinage bag <amount>` | Put an amount into a money bag. |
| `/coinage pay <player> <amount>` | Transfer purse funds when player payments are enabled. |
| `/coinage top [limit]` | Rank purse balances. Requires operator permission. |

## Shops and server settings

Stock a shop with goods to sell. Fund a pawn shop to buy goods from players. Use **Edit Offers** in a shop you manage to set templates and prices. The automation toggle controls whether item transfer systems may access the shop's stock. It starts disabled.

Server settings use Fzzy Config under `config/coinage/`. Merchant definitions are data driven and can be overridden by datapacks. Use `/coinage trades status` to see the loaded catalog and `/coinage trades reload` to reload it. Client settings include purse button visibility, HUD position, and balance notifications.

Coinage can import supported legacy Numismatic Overhaul purse balances from existing save data. Back up the world before switching. This purse import does not convert old physical coins, blocks, or shop inventories registered under the old mod namespace.

If you tested the earlier five-tier development build, deposit physical coins into purses before upgrading. Iron coins have been removed, and the physical values of silver, gold, and platinum have changed. Purse balances and money bags already store their value in bronze. Update clients and servers together.

## Build locally

On Windows, run `./scripts/build.ps1 -Loader both -Target all`. On Linux, run `bash scripts/build.sh both all`. The scripts build each selected target and verify the release JARs in `build/<version>/<loader>/libs/`. Artifact verification requires Python 3.11 or newer.

Coinage uses the [Bare Minimum License](LICENSE). See [NOTICE.md](NOTICE.md) and [LICENSE-UPSTREAM](LICENSE-UPSTREAM) for upstream attribution.
