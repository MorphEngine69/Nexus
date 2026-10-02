<div align="center">

# Nexus

**Network storage and automation for Minecraft.**
Build one network or many, fill it with items, fluids and energy, and let it craft, move and place things for you.

![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62b47a?style=flat-square)
![NeoForge](https://img.shields.io/badge/NeoForge-26.2.0.88-e68c3a?style=flat-square)
![Java](https://img.shields.io/badge/Java-25-5382a1?style=flat-square)
[![License: LGPL-3.0](https://img.shields.io/badge/License-LGPL--3.0-blue?style=flat-square)](LICENSE)
[![API: MIT](https://img.shields.io/badge/API-MIT-green?style=flat-square)](LICENSE-API)

[Features](#features) · [Requirements](#requirements) · [Building](#building) · [Structure](#project-structure) · [Contributing](#contributing) · [Changelog](CHANGELOG.md)

</div>

---

## About

Everything in a Nexus network hangs off one controller, the **Nexus**. Cables carry the network, devices join it by touching a cable, and what the network holds is reached from any terminal. Nothing travels through the cables as an entity: items, fluids and FE live in the network, and devices at its edges move them in and out.

A world can hold any number of independent networks, each with its own name and color, and a network can reach across dimensions.

## Features

### Network

- **Nexus** controller with a name and a color for its network, and figures for energy and devices.
- **Cables** in all 16 dye colors; cables of different colors run side by side without joining.
- **Energy** in FE: Energy Cells in any number, a Coal Generator, and a priority that decides which source fills first and which is drawn first.
- **Wireless**: a Network Transmitter and Receiver, linked by a Network Card, carry a network between places and dimensions.

### Storage

- **Storage Vault** holds 16 Vault Cells and has a priority.
- **Vault Cells** for items, fluids and energy, in six sizes from 1k to 512k, each with a whitelist or blacklist filter.

### Access

- **Terminal**, **Crafting Terminal** and **Blueprint Terminal**, mounted on a cable, with search, sorting and a resizable window.
- **Nexus Terminal** to carry, working anywhere a Nexus Link of its network reaches.
- Recipe transfer and drag-and-drop filters through **JEI** and **REI**.

### Transfer and world

- **Puller** and **Pusher** move items, fluids and FE between the network and the block they face, with filters, redstone modes, delivery order and keep-in-stock amounts.
- **Placer** and **Remover** place, drop, break and pick up in the space in front of them.
- Filters take whitelists and blacklists, tags, and looser matching that ignores wear or components.

### Autocrafting

- **Blueprints** encode crafting and machine recipes, with substitutes for items of a tag.
- **Assembler** crafts on its own or hands work to any block beside it, a furnace or a machine from another mod, and Assemblers can be chained onto one machine.
- Orders from a terminal show a plan first and can craft less when something is missing. **Crafting Monitor** lists running tasks.

### Upgrades

Speed, Stack, Regulator, Capacity, Range, Fortune, Silk Touch, Autocrafting and Chunk Loader. Each device takes the ones that make sense for it.

### Languages

English, Russian, Spanish, German and French.

## Requirements

| | |
|---|---|
| Minecraft | 26.2 |
| NeoForge | 26.2.0.88 |
| Java | 25 |

JEI or REI is optional and adds recipe transfer to the terminals.

## Installation

Put the mod jar into the `mods` folder of a NeoForge instance and start the game.

Nexus will be published on CurseForge and Modrinth. Until then, build the jar from source as described below.

## Building

```bash
git clone https://github.com/MorphEngine69/Nexus.git
cd Nexus
./gradlew build
```

The jar is written to `build/libs`.

| Task | Does |
|---|---|
| `./gradlew build` | Compiles, runs Checkstyle and the unit tests |
| `./gradlew runClient` | Starts a development client |
| `./gradlew runServer` | Starts a development server |
| `./gradlew runGameTestServer` | Runs the in-world game tests |
| `./gradlew :nexus-network:test` | Tests one module |

## Project structure

The core is plain Java and builds and tests without Minecraft; the game code sits on top of it and is packed into a single jar.

| Module | Role | License |
|---|---|---|
| `nexus-core-api` | Shared contracts and utilities | MIT |
| `nexus-resource-api` | Resource identity, amounts and filters | MIT |
| `nexus-storage-api` | Storage contracts and composition | MIT |
| `nexus-energy-api` | Energy buffers, sources and consumers | MIT |
| `nexus-transport-api` | Transfer quotas, redstone and ordering | MIT |
| `nexus-upgrade-api` | Upgrade contracts | MIT |
| `nexus-automation-api` | Blueprints, crafting plans and task states | MIT |
| `nexus-network-api` | Network, nodes and the graph | MIT |
| `nexus-network` | Network, storage, energy, transfer and crafting logic | LGPL-3.0 |
| `nexus-network-test` | Test fixtures for the core | LGPL-3.0 |
| root project | Blocks, items, menus, screens and game integration | LGPL-3.0 |

New kinds of resources, storages, upgrades and devices are added by registering them, without changes to the core.

## Contributing

Read [CONTRIBUTING.md](.github/CONTRIBUTING.md) first: it covers branches, commit messages, pull requests and code style. Bugs and ideas go to the [issue tracker](https://github.com/MorphEngine69/Nexus/issues).

## License

Nexus is licensed under the [LGPL-3.0](LICENSE).

The API modules (`nexus-*-api`) are licensed under the [MIT license](LICENSE-API), so addons can link against them under any license.
