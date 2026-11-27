"""Derive publishing metadata and stage the exact Coinage release JARs."""

import argparse
import json
import os
from pathlib import Path
import re
from shutil import copyfile


TARGETS = ("1.21.1", "26.3")
LOADERS = ("fabric", "neoforge")
RELEASE_DIRECTORY = Path("build/release")


def release_metadata():
    matrix = []
    versions = set()
    for target in TARGETS:
        properties = Path("targets", f"{target}.properties").read_text(encoding="utf-8")
        pins = dict(line.split("=", 1) for line in properties.splitlines() if "=" in line and not line.startswith("#"))
        version, _, game_version = pins["mod_version"].partition("+")
        if game_version != target or pins["minecraft_version"] != target:
            raise SystemExit(f"Minecraft version mismatch in the {target} pins.")
        versions.add(version)
        for loader in LOADERS:
            display_version = f"{version}+{target}-{loader}"
            matrix.append({
                "target": target,
                "loader": loader,
                "java": pins["java_version"],
                "version": display_version,
                "jar": f"coinage-{display_version}.jar",
            })

    if len(versions) != 1:
        raise SystemExit("Both Minecraft targets must use the same Coinage version.")
    version = versions.pop()
    if not re.fullmatch(r"\d+\.\d+\.\d+(?:-(?:dev|alpha|beta|rc)(?:[.-]?\d+)?)?", version):
        raise SystemExit(f"Unsupported Coinage release version: {version}")
    suffix = version.partition("-")[2]
    channel = "beta" if suffix.startswith(("beta", "rc")) else "alpha" if suffix else "release"
    return {"version": version, "tag": f"v{version}", "channel": channel, "matrix": {"include": matrix}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--publish", action="store_true", help="Require a matching GitHub release tag.")
    parser.add_argument("--stage", nargs=2, metavar=("MINECRAFT", "LOADER"), help="Stage a verified build.")
    args = parser.parse_args()
    metadata = release_metadata()

    if args.publish and os.environ.get("GITHUB_REF") != f"refs/tags/{metadata['tag']}":
        raise SystemExit(f"Publishing requires the {metadata['tag']} tag.")

    changelog = Path("CHANGELOG.md").read_text(encoding="utf-8")
    section = re.search(rf"^## {re.escape(metadata['version'])}[ \t]*\n(.*?)(?=^## |\Z)", changelog, re.MULTILINE | re.DOTALL)
    if not section or not section[1].strip():
        raise SystemExit(f"Add release notes for {metadata['version']} to CHANGELOG.md.")

    if args.stage:
        target, loader = args.stage
        entry = next((item for item in metadata["matrix"]["include"] if (item["target"], item["loader"]) == (target, loader)), None)
        if entry is None:
            parser.error(f"unsupported build: {target}/{loader}")
        source = Path("build", target, loader, "libs", f"coinage-{loader}-{metadata['version']}+{target}.jar")
        destination = RELEASE_DIRECTORY / entry["jar"]
        RELEASE_DIRECTORY.mkdir(parents=True, exist_ok=True)
        copyfile(source, destination)
        (RELEASE_DIRECTORY / "RELEASE-NOTES.md").write_text(section[1].strip() + "\n", encoding="utf-8")
        print(f"Staged {destination}")
        return

    print(json.dumps(metadata, indent=2))
    if output := os.environ.get("GITHUB_OUTPUT"):
        with Path(output).open("a", encoding="utf-8") as stream:
            for key, value in metadata.items():
                print(f"{key}={json.dumps(value, separators=(',', ':')) if key == 'matrix' else value}", file=stream)


if __name__ == "__main__":
    main()
