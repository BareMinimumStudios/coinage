"""Check the release artifacts built by the selected loader modules."""

import json
import os
import struct
try:
    import tomllib
except ModuleNotFoundError:
    raise SystemExit("Release verification requires Python 3.11 or newer.")
from pathlib import Path
from zipfile import ZipFile


mod_id = "coinage"
studio = "pokesmells, Bare Minimum Studios"
metadata_paths = {"fabric": "fabric.mod.json", "neoforge": "META-INF/neoforge.mods.toml"}


def verify(loader):
    target = os.getenv("COINAGE_TARGET", "1.21.1")
    if target not in {"1.21.1", "26.3"} or loader not in metadata_paths:
        raise SystemExit(f"Unsupported release target: {target}/{loader}")
    pins = dict(line.split("=", 1) for line in Path("targets", target + ".properties").read_text(encoding="utf-8").splitlines() if "=" in line and not line.startswith("#"))
    directory = Path("build", target, loader, "libs")
    jars = [
        path for path in directory.glob(f"coinage-{loader}-*.jar")
        if not path.stem.endswith(("-dev", "-sources", "-all", "-unshaded"))
    ]
    if len(jars) != 1:
        raise SystemExit(f"Expected one release JAR in {directory}. Clean stale builds and retry.")
    with ZipFile(jars[0]) as archive:
        entries = archive.namelist()
        names = set(entries)
        if len(entries) != len(names):
            raise SystemExit(f"{loader} contains duplicate archive entries")
        required = {
            "LICENSE", "LICENSE-UPSTREAM", "NOTICE.md", metadata_paths[loader], "coinage.mixins.json",
            "net/bms/coinage/PurseLedger.class",
            "net/bms/coinage/PiggyBankItem.class",
            "net/bms/coinage/PiggyBankColoringRecipe.class",
            "net/bms/coinage/PiggyBankEffects.class",
            "net/bms/coinage/BankTransactions.class",
            "assets/coinage/sounds/piggy_bank_break.ogg",
            "data/coinage/recipe/piggy_bank_coloring.json",
            "net/bms/coinage/PurseMenu.class",
            "net/bms/coinage/PurseTransactions.class",
            "net/bms/coinage/MerchantPayments.class",
            "net/bms/coinage/MerchantBagPricing.class",
            "net/bms/coinage/mixin/MerchantBagPricingMixin.class",
            "net/bms/coinage/mixin/VillagerBagDiscountMixin.class",
            "net/bms/coinage/mixin/MerchantPaymentsMixin.class",
            "net/bms/coinage/CoinageCreative.class",
            "net/bms/coinage/ShopEntity.class",
            "net/bms/coinage/ShopOffer.class",
            "net/bms/coinage/ShopClickGuard.class",
            "net/bms/coinage/ShopEditing.class",
            "net/bms/coinage/ShopEditPayloads.class",
            "net/bms/coinage/ShopEditorData.class",
            "net/bms/coinage/client/ShopClient.class",
            "net/bms/coinage/client/ShopEditScreen.class",
            "net/bms/coinage/mixin/ShopClickGuardMixin.class",
            "net/bms/coinage/CurrencyInteractions.class",
            "net/bms/coinage/CurrencyPayment.class",
            "net/bms/coinage/mixin/ItemStackCountMixin.class",
            "net/bms/coinage/mixin/StackSizeComponentMixin.class",
            "net/bms/coinage/mixin/ContainerStackLimitMixin.class",
            "net/bms/coinage/internal/crunch/Crunch.class",
            "net/bms/coinage/EconomyConfig.class",
            "net/bms/coinage/ChestLootConfig.class",
            "net/bms/coinage/ChestCurrencyLoot.class",
            "net/bms/coinage/SetMoneyFunction.class",
        }
        required.update("net/bms/coinage/" + name + ".class" for name in (
            "ShopStock", "TradeCatalog", "TradeDefinition", "TradePlatform", "TradeSpecializations", "MerchantCommands",
            "PurseMigration", "PurseSync", "PursePayloads", "PurseClientConfig", "ClientPurseState", "client/PurseClient",
            "CoinageFabric" if loader == "fabric" else "CoinageNeoForge",
            "CoinageFabricClient" if loader == "fabric" else "CoinageNeoForgeClient",
            "CoinageFabricNetwork" if loader == "fabric" else "CoinageNeoForgeNetwork",
            "ShopFabricAutomation" if loader == "fabric" else "ShopNeoForgeAutomation"
        ))
        if missing := required - names:
            raise SystemExit(f"{loader} is missing {', '.join(sorted(missing))}")
        if any(name.startswith("redempt/crunch/") for name in names):
            raise SystemExit(f"{loader} contains an unrelocated copy of Crunch")
        forbidden = ("io/wispforest/", "org/ladysnake/cca/", "dev/onyxstudios/cca/", "redempt/crunch/")
        if any(name.startswith(forbidden) for name in names):
            raise SystemExit(f"{loader} contains a removed or unrelocated runtime dependency")
        expected_major = int(pins["java_version"]) + 44
        for name in sorted(names):
            if name.startswith("net/bms/coinage/") and name.endswith(".class") and not name.startswith("net/bms/coinage/internal/"):
                header = archive.read(name)[:8]
                if header[:4] != b"\xca\xfe\xba\xbe" or struct.unpack(">H", header[6:8])[0] != expected_major:
                    raise SystemExit(f"Wrong Java class version in {name}")
            if name.endswith(".json") and name.startswith(("assets/coinage/", "data/coinage/")):
                json.loads(archive.read(name))
        for notice in ("LICENSE", "LICENSE-UPSTREAM", "NOTICE.md"):
            packaged = archive.read(notice).replace(b"\r\n", b"\n")
            if packaged != Path(notice).read_bytes().replace(b"\r\n", b"\n"):
                raise SystemExit(f"{loader} contains a stale {notice}")
        tiers = {"bronze", "iron", "silver", "gold", "platinum"}
        for tier in tiers:
            for suffix in ("", "_0", "_1", "_2", "_3"):
                texture = f"assets/coinage/textures/item/{tier}_coin{suffix}.png"
                if texture not in names:
                    raise SystemExit(f"Missing coin sprite: {texture}")
                png = archive.read(texture)
                if png[:8] != b"\x89PNG\r\n\x1a\n" or struct.unpack(">II", png[16:24]) != (16, 16):
                    raise SystemExit(f"Coin sprite must preserve the original 16x16 grid: {texture}")
        model_root = "assets/coinage/items/" if target == "26.3" else "assets/coinage/models/item/"
        coin_models = {name[len(model_root):-len("_coin.json")] for name in names if name.startswith(model_root) and name.endswith("_coin.json")}
        if coin_models != tiers:
            raise SystemExit(f"Unexpected currency tiers: {sorted(coin_models)}")
        profiles = [name for name in names if name.startswith("data/coinage/villager_trades/") and name.endswith(".json")]
        definitions = sum(len(level) for name in profiles for level in json.loads(archive.read(name))["trades"].values())
        if len(profiles) != 14 or definitions != 272:
            raise SystemExit(f"Incomplete default merchant catalog: {len(profiles)} profiles / {definitions} trades")
        metadata = archive.read(metadata_paths[loader]).decode("utf-8")
        if "${" in metadata:
            raise SystemExit(f"Unexpanded metadata in {loader}")
        if loader == "fabric":
            mod = json.loads(metadata)
            expected = {"fabricloader", "fabric-api", "minecraft", "java", "fabric-language-kotlin", "fzzy_config"}
            if mod["id"] != mod_id or mod["name"] != "Coinage" or mod["license"] != "BML-1.0" or mod["authors"] != studio.split(", ") or set(mod["depends"]) != expected:
                raise SystemExit("Unexpected Fabric metadata")
            if mod["version"] != pins["mod_version"] or mod["depends"]["minecraft"] != target or mod["depends"]["java"] != ">=" + pins["java_version"]:
                raise SystemExit("Fabric version constraints do not match the selected target")
            for side, entrypoint in (("main", "CoinageFabric"), ("client", "CoinageFabricClient")):
                if mod.get("entrypoints", {}).get(side) != [{"adapter": "kotlin", "value": "net.bms.coinage." + entrypoint}]:
                    raise SystemExit(f"Incorrect Fabric {side} entrypoint")
        else:
            mod = tomllib.loads(metadata)
            if mod["modLoader"] != "kotlinforforge" or mod["license"] != "BML-1.0":
                raise SystemExit("Incorrect NeoForge Kotlin loader or license")
            declaration = mod["mods"]
            if len(declaration) != 1 or declaration[0]["modId"] != mod_id or declaration[0]["displayName"] != "Coinage" or declaration[0]["authors"] != studio or declaration[0]["version"] != pins["mod_version"]:
                raise SystemExit("Unexpected NeoForge metadata")
            dependencies = {entry["modId"]: entry for entry in mod["dependencies"][mod_id]}
            if set(dependencies) != {"neoforge", "minecraft", "fzzy_config"} or dependencies["minecraft"]["versionRange"] != f"[{target}]":
                raise SystemExit("Unexpected NeoForge runtime dependencies")
            if any(entry["type"] != "required" or entry["side"] != "BOTH" for entry in dependencies.values()):
                raise SystemExit("Incorrect NeoForge dependency requirements")
    print(f"Verified {jars[0]}")


if __name__ == "__main__":
    selected = os.getenv("RELEASE_LOADER")
    for target in (selected,) if selected else metadata_paths:
        verify(target)
