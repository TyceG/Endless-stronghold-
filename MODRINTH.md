# Endless Stronghold

A world type where the Overworld is one vanilla stronghold, floating in the void, that never ends.

## Features

- **Vanilla's stronghold, exactly.** Every room is vanilla's piece, block for block: corridors, turns, prison halls, room crossings (pillar, fountain, storeroom), five-way crossings, chest corridors, libraries, spiral and straight staircases, and portal rooms. Cracked, mossy and infested bricks, vanilla's doors and vanilla's loot.
- **Grown the way vanilla grows one.** Piece by piece, with vanilla's weights, limits and rules, every doorway at its real height.
- **Endless.** The world is split into 256 × 256 block regions, each growing its own stronghold, joined into one by main corridors made of vanilla's pieces.
- **A portal room in every region.** Eyes of ender and `/locate structure endless_stronghold:portal_room` find the nearest one. The End is vanilla.
- **Fast.** About five times faster to generate than a normal Overworld.

## How to play

Install Fabric Loader and Fabric API for Minecraft 26.3, add this mod, and choose **World Type: Endless Stronghold** when you create a world. You spawn in a lit pillar room; monsters spawn in the dark. Throw an eye of ender to find a portal room.

For a server, set `level-type=endless_stronghold:endless_stronghold` in `server.properties` before the world is created.

## Settings

The world preset's `hub_y` (default 30) and `lowest_y` (default -24) set how high the stronghold sits. Change them with a data pack that replaces `data/endless_stronghold/worldgen/world_preset/endless_stronghold.json`.

## Requirements

- Minecraft Java Edition 26.3
- Fabric Loader 0.20.1 or newer
- Fabric API
- Java 25

Source and issues: https://github.com/TyceG/Endless-stronghold-
