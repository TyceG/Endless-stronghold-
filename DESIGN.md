# Endless Stronghold: Fabric Mod Design Doc (Minecraft 26.3)

Nov 14, 2026 · @Tyce · revised Nov 14, 2026 for vanilla-style growth

## Overview

Endless Stronghold is a Fabric mod for Minecraft Java Edition 26.3 that adds a world type where the Overworld is one stronghold, floating in the void, that never ends. It grows the way vanilla grows a stronghold, piece by piece, from vanilla's own pieces, and it keeps growing in every direction. The same seed always gives the same stronghold.

| Item | Value |
|---|---|
| Mod name | Endless Stronghold |
| Mod id | `endless_stronghold` |
| Minecraft | Java Edition 26.3 |
| Loader | Fabric, with Fabric API |
| Install location | The game for singleplayer, the server for multiplayer |
| Player entry point | World Type → Endless Stronghold on the Create World screen |

Goals:

- Exactly like a vanilla stronghold: vanilla's pieces block for block, grown by vanilla's algorithm with vanilla's weights, limits and rules, every doorway at its real height.
- Endless in every horizontal direction, out to the world border.
- One connected stronghold: the whole thing can be walked from spawn, apart from the few branches vanilla itself leaves cut off.
- A portal room in every region, which eyes of ender and `/locate` find.
- Deterministic and seamless: chunks generated in any order, on any thread, fit together.

Not in version 1:

- New blocks, items or mobs.
- Changes to the Nether or the End.
- Bedrock Edition.
- An in-game settings screen.

## How to hand this to Claude Code

Open a Claude Code session on the repository, give it this doc, and ask for the next phase of the build plan. Rules for Claude Code:

- Work one phase at a time. A phase is done when CI is green and the jar is ready to test.
- Run Gradle on JDK 25.
- Treat Minecraft class and method names from older versions as hints. Check each one against the 26.3 sources (Loom's `genSources`).
- Every room must be vanilla's piece, block for block. Copy piece code from 26.3's `StrongholdPieces` through `rooms.Piece`, which applies vanilla's own orientation transform.
- Keep the growth logic in plain Java with no Minecraft imports, so it can be unit-tested.
- Don't add features outside this doc without asking. Log any change of plan in `DECISIONS.md`.

## Toolchain and versions

| Component | Version | Notes |
|---|---|---|
| Minecraft | 26.3 | Unobfuscated: Mojang's names |
| Java (JDK) | 25 | |
| Fabric Loom | 1.19.4 | Plugin id `net.fabricmc.fabric-loom` (no remapping) |
| Gradle | 9.8.2 | From the official 26.3 example mod |
| Fabric Loader | 0.20.1 | |
| Fabric API | 0.166.2+26.3 | |

Since 26.1 the build uses `implementation` (not `modImplementation`) and builds `jar` (not `remapJar`). A GitHub Actions workflow runs `./gradlew build` on JDK 25 and uploads `build/libs/*.jar`.

## Decisions and defaults

| Decision | Default | Why |
|---|---|---|
| Overworld | One endless stronghold in the void | No terrain, ores, trees or vanilla structures |
| Nether and End | Vanilla, unchanged | Portal rooms lead to a normal dragon fight |
| Generation | Vanilla's StrongholdPieces algorithm | Rooms, doorways and heights exactly as vanilla makes them |
| Region | 256 × 256 blocks (16 × 16 chunks); region (0, 0) centred on the origin | Each region grows one vanilla stronghold; vanilla's are about 225 across |
| Joining regions | Main corridors of vanilla pieces from each region's centre to the middle of each edge | So the tree never ends |
| Height | Each region's hub floor at Y 30; pieces stay above Y -24 | Vanilla's range: 54 blocks below its start |
| Portal rooms | 1 per region | Vanilla's limit is 1 per stronghold, and vanilla starts over until there is one |
| Space between rooms | Void | Breaking a wall shows the void |
| Doorways leading nowhere | Stone just beyond them | Underground, vanilla's look onto stone |
| Mobs | Overworld monsters spawn in dark rooms; no animals | Source of ender pearls, string, bones and gunpowder |
| Loot | Vanilla's stronghold loot tables, unchanged | As in a vanilla stronghold |
| Spawn | The spawn region's hub: a lit pillar room at (0, 31, -3); respawn radius 0 | Players never land on the roof |
| Settings | `hub_y` (30) and `lowest_y` (-24) in the world preset JSON, changed with a data pack | No settings screen in version 1; region size fixed |

## World setup

One world preset gives the Overworld the mod's generator and leaves the Nether and End vanilla. Everything except the generator code is data in the mod jar.

| File under `src/main/resources/data/` | Purpose | Status |
|---|---|---|
| `endless_stronghold/worldgen/world_preset/endless_stronghold.json` | Overworld uses the mod's generator and one fixed biome; Nether and End are copied from vanilla's `minecraft:normal` preset | Done |
| `minecraft/tags/worldgen/world_preset/normal.json` | Adds the preset to the World Type list, with `"replace": false` | Done |
| `endless_stronghold/worldgen/biome/stronghold_void.json` | The only Overworld biome: no features, no carvers, the monster spawns from `minecraft:plains` | Done |
| `endless_stronghold/worldgen/structure/portal_room.json` | A marker structure, so eyes of ender and `/locate` have something to look for (see Portal rooms) | Done |
| `minecraft/tags/worldgen/structure/eye_of_ender_located.json` | Adds the marker structure, with `"replace": false` | Done |

Overworld details:

- Generator type `endless_stronghold:endless_stronghold`, with a `minecraft:fixed` biome source set to `endless_stronghold:stronghold_void`.
- Dimension type stays `minecraft:overworld`: Y -64 to 319, normal days and nights, and beds work.
- No vanilla structure lists the biome, so none generate, including the vanilla stronghold.
- Dedicated servers: set `level-type=endless_stronghold:endless_stronghold` in `server.properties` before the world is first created.

## Generation

The stronghold is grown region by region, each with vanilla's own algorithm, and the regions are joined by main corridors so it never ends. Code: `gen.Grower`, `gen.Region`, `gen.Stronghold`.

### Growing a region, as vanilla does

Vanilla's `StrongholdPieces`, step for step:

1. A list of pending pieces is worked through in random order. Each piece tries to attach a child at each of its doorways (forward, left, right, as its `addChildren` says), at that doorway's exact position and height. There are no floors: heights wander, and staircases go down.
2. The child's kind is rolled by vanilla's weights, with up to 5 tries. A kind is skipped if it's used up, if it was the last kind placed (no two the same in a row), or if it's a library less than 5 pieces deep or a portal room less than 6 deep.
3. The child is placed only if its box overlaps no other piece, stays above the lowest height, and stays inside the region (vanilla: within 112 blocks of its start). Nothing deeper than 50 pieces is placed.
4. If no kind fits, vanilla's filler corridor runs up to the piece in the way, breaking one block into it. If even that fails, the doorway is left leading nowhere.
5. Each piece's random properties (its door, its side exits, its room type, a library's height) are rolled as vanilla's constructors roll them.
6. If the region ends up with no portal room, it starts over (up to 20 times), as vanilla does.

Vanilla's quirks come too. A five-way crossing facing north or west places its side children at the wrong heights, so a few branches can't be walked into, as in vanilla.

### Main corridors

Instead of vanilla's starting spiral staircase, each region starts from its main corridors:

- **The hub:** a room crossing at the region's centre, its floor at Y 30.
- **Four lines out:** from each of the hub's four doorways, a line of vanilla's straight-through pieces runs to the middle of that edge. The pieces are straight corridors, room crossings, prison halls, chest corridors and both kinds of staircase, rolled with vanilla's weights.
- **Meeting the next region:** each line is exactly as long as it needs to be, with 0, 1 or 2 staircases (fixed for each region edge). So it ends on the edge in line and level with the neighbouring region's line, and the two open into each other.

Everything else grows from the main corridors' doorways, as vanilla grows from its start, so each region is one tree and the regions join into one endless stronghold.

### Doorways that lead nowhere

Underground, a vanilla doorway with nothing beyond it looks onto stone. In the void it would look onto nothing. So each block just outside such a doorway that no piece covers becomes stone. A main corridor's last doorway meets the next region's and stays open.

### Determinism

- All randomness comes from stateless hashes of the world seed, the region and a salt (SplitMix64). Nothing depends on the order chunks generate in.
- A region is grown whole, the first time any of its chunks needs it, and kept in a small thread-safe cache. Growing one takes a few milliseconds.
- Each piece draws its own randomness (block mix, torches, cobwebs, portal eyes) from its own seed, so a piece crossing chunk borders comes out the same in each.

## Room catalog

All of vanilla's stronghold pieces, block for block, with vanilla's weights and per-stronghold (here, per-region) limits.

| Piece | Size (w × h × d) | Weight | Limit | Notes |
|---|---|---|---|---|
| Straight corridor | 5 × 5 × 7 | 40 | none | Random side exits; 1 in 10 chance of a torch at each of 4 spots |
| Prison hall | 9 × 5 × 11 | 5 | 5 | 2 cells behind iron bars and iron doors |
| Left turn, right turn | 5 × 5 × 5 | 20 each | none | |
| Room crossing | 11 × 7 × 11 | 10 | 6 | Pillar room, fountain room, storeroom with balcony and chest, or empty |
| Straight stairs | 5 × 11 × 8 | 5 | 5 | Cobblestone stairs, down 6 |
| Spiral stairs | 5 × 11 × 5 | 5 | 5 | Down 6 |
| Five-way crossing | 10 × 9 × 11 | 5 | 4 | Exits at three heights |
| Chest corridor | 5 × 5 × 7 | 5 | 4 | Chest in a raised niche |
| Library | 14 × 6 or 11 × 15 | 10 | 2 | Only 5+ deep; tall with balcony, chandelier and second chest when it fits |
| Portal room | 11 × 8 × 16 | 20 | 1 | Only 6+ deep; end portal frames, lava, silverfish spawner |
| Filler corridor | 5 × 5 × 1 to 3 | (fallback) | none | When nothing else fits |
| Hub (spawn room) | 11 × 7 × 11 | (fixed) | | Region (0, 0)'s hub is the pillar room, with 8 extra torches and no infested bricks |

## Blocks, loot and mobs

Blocks:

- Walls, floors and ceilings use vanilla's stronghold block selector: 20% cracked, 30% mossy and 5% infested stone bricks, the rest plain stone bricks, chosen by a position hash. The spawn room has no infested bricks.
- Doors are each piece's own front door, one of vanilla's small door types with vanilla's odds: 2 in 5 open arches, then 1 in 5 each of oak door, iron bars and iron door with a stone button on each side.
- Inside air is cave air, as in vanilla.
- Blocks whose shape depends on their neighbours (iron bars, fences, ladders, torches) and fluids are marked for vanilla's post-processing, as vanilla's structure pieces do.

Loot:

Chests use vanilla's own stronghold loot tables, unchanged:

| Chests in | Loot table |
|---|---|
| Chest corridors | `minecraft:chests/stronghold_corridor` |
| Storerooms | `minecraft:chests/stronghold_crossing` |
| Libraries | `minecraft:chests/stronghold_library` |

- Chests are placed in the decoration stage, turned the way vanilla's structure chests are, and store their loot table and a seed. Items roll the first time a player opens them, as in vanilla.

Mobs:

- The biome's spawn list is the monster list from `minecraft:plains`, with no animals. Monsters spawn only in darkness, so the lit spawn room stays safe.
- Each portal room has a silverfish spawner on its stairs, as in vanilla.

## Portal rooms, eyes of ender and spawn

### Portal rooms

Every region's growth places exactly one portal room, vanilla's piece block for block. It has 12 end portal frames, each with a 10% chance of a pre-placed eye (the portal lights only if all 12 have one), lava, iron bar windows, and a silverfish spawner. These are built already.

### Eyes of ender and /locate

Eyes of ender and `/locate structure` both ask the world's chunk generator for the nearest structure of a kind (`ChunkGenerator.findNearestMapStructure`). The mod's generator can answer that itself, from the regions' plans:

1. Register a structure type and a marker structure, `endless_stronghold:portal_room`, that never generates anything by itself. The portal rooms are already built by the growth.
2. Add the marker structure to `#minecraft:eye_of_ender_located`.
3. In the generator, override `findNearestMapStructure`: when asked for that structure, return the portal room closest to the player, searching outward region by region. Anything else goes to vanilla.

This needs no structure set and no mixin, because the portal room's position comes straight from the region's plan. It works for regions that haven't been visited yet. (This replaces the original plan of a `random_spread` structure set, which can't match where the growth puts each portal room.)

### Spawn

- The spawn point is in region (0, 0)'s hub, a pillar room, at (0, 31, -3): on the floor between its north doorway and the pillar.
- When a new world of this type first loads, the world spawn is set there, and the respawn radius game rule (`respawn_radius`) is set to 0 (`MinecraftServerMixin`).
- Vanilla moves joining and respawning players to the highest block in the spawn column, which here is the roof. `PlayerSpawnFinderMixin` keeps them at the world spawn, loading its chunk first. Beds work as usual.

## Code structure

Growth is plain Java with no Minecraft imports. The Minecraft-facing classes are thin wrappers.

| Component | Job | Minecraft hook |
|---|---|---|
| `EndlessStronghold` | Registers the generator codec; sets up spawn | Mod initializer |
| `EndlessStrongholdGenerator` | Builds each chunk's share of its region's pieces | Extends `ChunkGenerator`, modelled on `FlatLevelSource` |
| `gen.Grower` | Grows one region: main corridors, then vanilla's growth | None |
| `gen.Region`, `gen.Stronghold` | A region's pieces and stone plugs; the region cache | None |
| `gen.Part`, `gen.Kind`, `gen.Box`, `gen.Frame` | A placed piece; vanilla's piece kinds, boxes and coordinate transform | None |
| `rooms.Renderer`, `rooms.Piece` | Each piece's vanilla postProcess, line for line, through vanilla's transform | None |
| `world.ChunkSink`, `world.DecorationSink` | Write a piece's blocks into a chunk, clipped; chests and spawners in the decoration stage | `ChunkAccess`, `WorldGenLevel` |
| `mixin.MinecraftServerMixin`, `mixin.PlayerSpawnFinderMixin` | Spawn in the spawn room | Mixins |

### Generation stages

- `buildTerrain`: every block of the pieces that reach into the chunk, and the stone plugs, written straight into the chunk without neighbour updates, updating the generation heightmaps the way `FlatLevelSource` does.
- `applyBiomeDecoration`: call the parent method first, then place the blocks that need block entities: chests (loot table and seed) and the portal room's spawner (silverfish).
- `spawnOriginalMobs` does nothing. `getBaseHeight` and `getBaseColumn` describe an empty column.

### Threading and speed

- Several threads generate chunks at once. No static mutable state; region plans are immutable and kept in a bounded, lock-free cache.
- Measured on a dev server, the same 1,089 chunks and seed: about 4.3 seconds as an Endless Stronghold world, about 21.3 seconds as a normal Overworld (`./gradlew runBenchServer`). About five times faster.

## Testing and acceptance criteria

A phase is done when its unit tests pass, the CI build is green, and the player checklist items for that phase pass.

### Unit tests (JUnit, no Minecraft)

- Determinism: one seed gives the same regions twice; two seeds differ.
- Vanilla's rules in every region: pieces inside the region and above the lowest height, no overlaps except a filler corridor's one block, vanilla's limits, depth at most 51, exactly one portal room.
- Main corridors: at every region edge, the two regions' corridors touch, in line and level.
- Walking: from the spawn point, across the spawn region and the next one, a player can walk into every main corridor piece, the next region's hub, and over 85% of all pieces (98 to 99% in practice).
- No air touches the void; every torch, ladder and button hangs on a solid block (or on a wall a filler corridor broke through, which the game then removes, as in vanilla); doors are whole.
- The spawn room has a floor and headroom at the spawn point, 12 torches and no infested bricks.
- Portal rooms: the nearest-portal-room search agrees with checking every region nearby, and the nearest is never more than a region and a half away.

### In-game smoke test

`./gradlew runSmokeServer`: a headless dev server builds a fresh world and checks that every chunk within 256 blocks of spawn generates, that the spawn point is in the spawn room (also when its chunk wasn't loaded yet), that there's a silverfish spawner, that a chest of each loot table rolls items, and that an eye of ender and `/locate` point into the nearest portal room, whose frames, filled with eyes, vanilla recognises and lights as a portal. It needs `run/smoke/eula.txt`, so it isn't in CI.

### CI

GitHub Actions runs `./gradlew build` (with the unit tests) on JDK 25 for every push and uploads the jar.

### Player checklist

1. Create a world with World Type set to Endless Stronghold. You spawn inside a lit room.
2. Explore: rooms are vanilla's, doorways lead to rooms or end at stone, and no hole opens onto the void.
3. Follow a main corridor out of the spawn region (about 128 blocks north, south, east or west) and carry on into the next region's stronghold.
4. In spectator mode, fly outside. There is only the stronghold, floating in the void.
5. Throw an eye of ender. It flies toward the nearest portal room. `/locate structure endless_stronghold:portal_room` finds it too.
6. Fill a portal and go through: the End and the dragon fight are vanilla.
7. Quit and reload: everything matches. A new world with the same seed has the same stronghold.
8. Flying around at render distance 12 doesn't stall chunk loading.
9. On a dedicated server with the mod, a player without the mod can join and play.

## Build plan

1. **Phase 1: walkable skeleton.** Done. Project, CI, world preset and biome, generator, spawn room and spawn fix.
2. **Phase 2: the real stronghold.** Done. Grown like vanilla, endless, every vanilla piece block for block, block palette, doors, chests with vanilla loot, mob spawning, portal rooms with their spawners. (See `DECISIONS.md` for how it got here: first floors and a maze, then vanilla's growth.)
3. **Phase 3: finding the End.** Done. The marker structure, its tag, and the generator's `findNearestMapStructure` override: eyes of ender and `/locate` find the nearest portal room, and its frames, filled with eyes, light as a vanilla portal.
4. **Phase 4: polish.**
   - Done: generator settings `hub_y` and `lowest_y` in the preset (tested through a data pack); the speed check (about five times faster than a normal Overworld); a new icon; a README with a map of the stronghold; `MODRINTH.md` (the Modrinth page text), `CHANGELOG.md`, contact links in `fabric.mod.json`, and a workflow that attaches the jar to a GitHub release when a version tag is pushed.
   - Left: confirm checklist 9 (a player without the mod joins a server with it); in-game screenshots for the README and the Modrinth gallery; publishing 1.0.0 (a version tag for the GitHub release, and uploading to Modrinth, which needs a Modrinth account).
   - Done when: the full checklist passes on a fresh world.

## Risks and gotchas

- **Old API names.** Most modding examples online predate 26.1 and use Yarn names. Check every name against the 26.3 sources.
- **Vanilla's transform.** Vanilla writes its piece code for its own orientation transform (north flips z, south mirrors block states, and so on). Copy pieces through `rooms.Piece`, never with a hand-made rotation, or torches, ladders and buttons end up on the wrong side.
- **Skipping the parent decoration call.** `applyBiomeDecoration` must call the parent method first.
- **Block entities placed too early.** Chests and spawners go in the decoration stage, not in `buildTerrain`.
- **Writing outside the chunk.** Every piece is clipped to the chunk being built; a piece crossing chunk borders is built a part at a time, with the same randomness each time.
- **Hidden randomness.** A shared `Random`, or anything depending on chunk generation order, causes seams that show up only on some runs.
- **Spawning on the roof.** Vanilla's spawn search picks the highest block; the spawn mixins keep players in the spawn room.
- **Changing the growth changes worlds.** Any change to the weights, the region size or the main corridors reshapes every existing world. Fine during development; after a release, it needs care.

## Installing and playing

1. Install Fabric Loader for Minecraft 26.3 with the installer from fabricmc.net.
2. Download Fabric API for 26.3, and the Endless Stronghold jar from the GitHub Actions run or a release. An Actions download comes as a .zip; unzip it to get the jar.
3. Put both jars in the `mods` folder inside your Minecraft folder (or a separate game directory for this profile).
4. Start the Fabric 26.3 profile and choose Create New World. On the World tab, set World Type to Endless Stronghold.
5. For a server, put both jars in the server's `mods` folder and set `level-type=endless_stronghold:endless_stronghold` before the world is created.

## Sources

- Minecraft 26.3's `StrongholdPieces`, `StrongholdStructure`, `StructurePiece` and `BoundingBox`, decompiled with Loom
- Fabric for Minecraft 26.3, Fabric blog, 15 September 2026
- Fabric for Minecraft 26.1, Fabric blog, 14 March 2026
