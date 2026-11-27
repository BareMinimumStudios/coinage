# Changelog

All notable changes to Coinage are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and Coinage uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html) for mod versions.

## [Unreleased]

## [0.1.1-dev] - 2026-11-27

### Changed

- Shared market price quotes now use a running supply total, so looking up a price avoids scanning every traded item.
- Build and publish scripts validate the changelog entry for the current version. Modrinth, CurseForge, and GitHub Releases share those notes.

## [0.1.0-dev] - 2026-11-27

### Added

- First Coinage alpha for Minecraft 1.21.1 and 26.3 on Fabric and NeoForge.
- Earn, carry, and exchange bronze, silver, gold, and platinum coins. Each tier stacks to 100 and defaults to a 100:1 exchange rate with the next tier.
- Store money in your purse or piggy bank, withdraw coins or money bags, and opt in to automatic coin collection.
- Set up player shops, pawn shops, and merchant trades. Server owners can enable shared supply and demand pricing with a configurable reset interval.
- Open Coinage shops through Easy NPC or apply market prices to eligible Easy NPC trades.
- Configure the economy through Fzzy Config and customize merchant offers and loot through datapacks.

[Unreleased]: https://github.com/BareMinimumStudios/coinage/compare/v0.1.1-dev...HEAD
[0.1.1-dev]: https://github.com/BareMinimumStudios/coinage/compare/7c9dbe9...v0.1.1-dev
[0.1.0-dev]: https://modrinth.com/mod/coinage-poke097/versions
