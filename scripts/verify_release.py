"""Check the release artifacts built by the selected loader modules."""

import json
import os
from pathlib import Path
from zipfile import ZipFile


mod_id = "numismatic_overhaul"
studio = "pokesmells, Bare Minimum Studios"
metadata_paths = {"fabric": "fabric.mod.json", "neoforge": "META-INF/neoforge.mods.toml"}


def verify(loader):
    directory = Path(loader, "build", "libs")
    jars = [
        path for path in directory.glob(f"numismatic-overhaul-{loader}-*.jar")
        if not path.stem.endswith(("-dev", "-sources", "-all", "-unshaded"))
    ]
    if len(jars) != 1:
        raise SystemExit(f"Expected one release JAR in {directory}. Clean stale builds and retry.")
    with ZipFile(jars[0]) as archive:
        names = set(archive.namelist())
        required = {
            "LICENSE", "LICENSE-Crunch", metadata_paths[loader],
            "net/bms/numismatic_overhaul/PurseLedger.class",
            "net/bms/numismatic_overhaul/internal/crunch/Crunch.class",
            "net/bms/numismatic_overhaul/EconomyConfig.class",
        }
        if missing := required - names:
            raise SystemExit(f"{loader} is missing {', '.join(sorted(missing))}")
        if any(name.startswith("redempt/crunch/") for name in names):
            raise SystemExit(f"{loader} contains an unrelocated copy of Crunch")
        metadata = archive.read(metadata_paths[loader]).decode("utf-8")
        if loader == "fabric":
            mod = json.loads(metadata)
            expected = {"fabricloader", "fabric-api", "minecraft", "java", "fabric-language-kotlin", "fzzy_config"}
            if mod["id"] != mod_id or mod["authors"] != studio.split(", ") or set(mod["depends"]) != expected:
                raise SystemExit("Unexpected Fabric metadata")
        elif f'modId="{mod_id}"' not in metadata or f'authors="{studio}"' not in metadata or 'modId="fzzy_config"' not in metadata:
            raise SystemExit("Unexpected NeoForge metadata")
    print(f"Verified {jars[0]}")


if __name__ == "__main__":
    selected = os.getenv("RELEASE_LOADER")
    for target in (selected,) if selected else metadata_paths:
        verify(target)
