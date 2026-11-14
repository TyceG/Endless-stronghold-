# Endless Stronghold: Fabric Mod Design Doc (Minecraft 26.3)

Nov 14, 2026 · @Tyce

## Overview

Endless Stronghold is a Fabric mod for Minecraft Java Edition 26.3 that adds a world type where the Overworld is one stronghold, floating in the void, that never ends. Each chunk builds its own piece of the stronghold from the world seed and its coordinates. That way the halls always line up, and the same seed always gives the same stronghold.

| Item | Value |
|---|---|
| Mod name | Endless Stronghold |
| Mod id | `endless_stronghold` |
| Minecraft | Java Edition 26.3 |
| Loader | Fabric, with Fabric API |
| Install location | The game for singleplayer, the server for multiplayer |
| Player entry point | World Type → Endless Stronghold on the Create World screen |

Goals:

- Endless in every horizontal direction, out to the world border, across 4 floors joined by stairs.
- Every room reachable from spawn, with no sealed-off sections.
- Looks and plays like a vanilla stronghold: stone brick halls, libraries, prison cells, fountain rooms, portal rooms, and chests with stronghold loot.
- Deterministic and seamless: chunks generated in any order, on any thread, fit together.

Not in version 1:

- New blocks, items or mobs.
- Changes to the Nether or the End.
- Bedrock Edition.
- An in-game settings screen.

## How to hand this to Claude Code

Open a Claude Code session on your GitHub repository, then give it this doc and the kickoff prompt below. It saves the doc as DESIGN.md and builds the mod one phase at a time.

1. Get the doc as text: click this doc's name, then Export → Markdown. In the phone app, Export is under Share.
2. Let Claude Code download the game files. On claude.ai/code, open your cloud environment's settings and set Network access to Custom. Check *Also include default list of common package managers*, then add the domains below to Allowed domains. Full also works.
3. Give it Java 25. The cloud machine comes with Java 21, so put this in the environment's Setup script: `apt update && apt install -y openjdk-25-jdk-headless`
4. Start a session on the repository, attach or paste the Markdown, and send the kickoff prompt.

Without steps 2 and 3, Claude Code can still write and push the code, and the GitHub Actions build compiles it.

Domains to allow (Maven Central and Gradle are already on the default list):

- `maven.fabricmc.net`
- `meta.fabricmc.net`
- `piston-meta.mojang.com`
- `piston-data.mojang.com`
- `libraries.minecraft.net`
- `resources.download.minecraft.net`

Kickoff prompt:

> Save the design doc I attached as DESIGN.md in this repository. Then build the Endless Stronghold Fabric mod it describes, for Minecraft 26.3, starting with Phase 1 of the build plan. Set the project up from the official Fabric example mod and add the GitHub Actions build. Stop after each phase and tell me how to download the jar so I can test it in the game.

Rules for Claude Code:

- Work one phase at a time. A phase is done when CI is green and the jar is ready to test.
- Run Gradle on JDK 25. If the machine still defaults to Java 21, point `JAVA_HOME` at the JDK 25 install.
- Treat the Minecraft class and method names in this doc as hints from older versions. Check each one against the 26.3 sources before using it.
- Keep the layout logic in plain Java with no Minecraft imports, so it can be unit-tested.
- Don't add features outside this doc without asking. Log any change of plan in `DECISIONS.md`.

## Toolchain and versions

Build with Java 25, Fabric Loom 1.17 and Gradle 9.6.0, the toolchain Fabric recommends for Minecraft 26.3 (Fabric for Minecraft 26.3). Minecraft 26.3 came out on 15 September 2026 and requires Java 25 (26.3 overview).

| Component | Version | Notes |
|---|---|---|
| Minecraft | 26.3 | "Wilderness Bound"; data pack format 121.0 |
| Java (JDK) | 25 | |
| Fabric Loom | 1.17 | Gradle plugin id `net.fabricmc.fabric-loom` |
| Gradle | 9.6.0 or newer | |
| Fabric Loader | 0.19.5 or newer | |
| Fabric API | Newest 26.3 build listed on fabricmc.net/develop | Depend on `fabric-api`, not `fabric` |

Minecraft has shipped unobfuscated since 26.1, so the build differs from older tutorials (Fabric for Minecraft 26.1):

- Use the `net.fabricmc.fabric-loom` plugin, which does not remap, and drop the `mappings` line.
- Use `implementation` and `compileOnly`, not `modImplementation`. Build `jar`, not `remapJar`.
- Use Mojang's official names. Yarn names from older guides won't compile.
- Look names up at mcsrc.dev, Fabric's online viewer of decompiled Minecraft source, or with Loom's `genSources` task.

26.3 also reworked parts of the world generation data: configured features moved to `worldgen/feature`, surface rules became material rules, and several placement modifiers were renamed. Copy any vanilla JSON this mod needs from the 26.3 jar, not from older examples.

Project setup: start from the official Fabric example mod for 26.3. Add a GitHub Actions workflow that runs `./gradlew build` on JDK 25 and uploads `build/libs/*.jar` as an artifact.

## Decisions and defaults

These are the starting choices. Change any row before Claude Code starts; anything not listed follows vanilla stronghold behaviour.

| Decision | Default | Why |
|---|---|---|
| Overworld | One endless stronghold in the void | No terrain, ores, trees or vanilla structures |
| Nether and End | Vanilla, unchanged | Portal rooms lead to a normal dragon fight |
| Floors | 4 floors, with floor blocks at Y 0, 16, 32 and 48 | Height is capped by the world; width is not |
| Cell | 16 × 16 blocks: one chunk on one floor | Each chunk writes only its own blocks |
| Region | 8 × 8 cells (128 × 128 blocks) | One maze per region |
| Loops | 15% of the walls the maze leaves get opened | A pure tree maze doesn't feel like a stronghold |
| Stairs | 2 staircases between each pair of floors, per region | Every floor stays reachable |
| Portal rooms | 1 per region, on a random floor | Eyes of ender lead to the nearest one |
| Space between rooms | Void; rooms and halls have 1-block walls | Breaking a wall shows the void |
| Mobs | Overworld monsters spawn in dark halls; no animals | Source of ender pearls, string, bones and gunpowder |
| Loot | Vanilla stronghold tables plus a small extra pool | Extra pool: flint for flint and steel, and a rare eye of ender |
| Spawn | A lit room at chunk (0, 0) on floor 1; spawn radius 0 | Players never land on the roof |
| Settings | Generator settings live in the world preset JSON | No settings screen in version 1 |

## World setup

One world preset gives the Overworld the custom chunk generator and leaves the Nether and End vanilla. Everything except the generator code is data in the mod jar.

| File under `src/main/resources/data/` | Purpose |
|---|---|
| `endless_stronghold/worldgen/world_preset/endless_stronghold.json` | Overworld uses the mod's generator and one fixed biome; Nether and End are copied from vanilla's `minecraft:normal` preset |
| `minecraft/tags/worldgen/world_preset/normal.json` | Adds the preset to the World Type list, with `"replace": false` |
| `endless_stronghold/worldgen/biome/stronghold_void.json` | The only Overworld biome: no features, no carvers, monster spawns |
| `endless_stronghold/worldgen/structure/portal_room.json` | The portal room structure (see Portal rooms) |
| `endless_stronghold/worldgen/structure_set/portal_rooms.json` | Places one portal room per region |
| `minecraft/tags/worldgen/structure/eye_of_ender_located.json` | Lets eyes of ender find portal rooms, with `"replace": false` |
| `endless_stronghold/loot_table/chests/*.json` | Chest tables: vanilla stronghold loot plus the extra pool |

Overworld details:

- Generator type `endless_stronghold:endless_stronghold`, with a `minecraft:fixed` biome source set to `endless_stronghold:stronghold_void`.
- Dimension type stays `minecraft:overworld`: Y -64 to 319, normal days and nights, and beds work.
- Build the biome from vanilla's 26.3 `minecraft:the_void` JSON. Remove its start platform feature so the features list is empty, and add the monster spawns from `minecraft:plains`.
- No vanilla structure lists this biome, so none generate, including the vanilla stronghold. Don't add the biome to any `has_structure/*` tag.
- Dedicated servers: set `level-type=endless_stronghold:endless_stronghold` in `server.properties` before the world is first created.

## Layout algorithm

The stronghold is a grid of cells, one per chunk per floor, wired together by a maze worked out for each region of 8 × 8 chunks. Any chunk can build its cells from the seed alone, and two neighbours always agree on the doorway between them.

### Cells and floors

- A cell is one chunk on one floor, with coordinates `(cx, cz, f)`: the chunk coordinates plus floor `f` from 0 to 3.
- Floor `f` has its floor blocks at `Y = baseY + 16f` (`baseY = 0`) and keeps everything within the 16 blocks from there up.
- Cell code works in local coordinates: x and z from 0 to 15, and y from 0 to 15 above the floor.

### Cell layout (top view)

- The room area is `x, z = 1..15`: a 15 × 15 square centred on block (8, 8). Rotating it by 90° about (8, 8) maps it onto itself.
- Every room side has its doorway at 7..9: 3 wide and 3 tall above the floor.
- Column `x = 0` is the connector to the west neighbour, and row `z = 0` the connector to the north. If that edge is open, the connector is a short hall: floor, ceiling, walls at 6 and 10, and the door across 7..9. If it is closed, the strip stays air.
- The east and south links are the neighbours' own connectors, so no chunk ever writes outside itself.
- Halls are 5 wide (walls at 6 and 10, walkway 7..9) and 3 tall inside, like vanilla corridors.

Every doorway sits on blocks 7 to 9, so a hall leaving any side meets the neighbour's connector exactly.

### Edges

- An edge joins two neighbouring cells. Its key is `(cx, cz, f, dir)`, taken from the west, north or lower cell, with `dir = EAST`, `SOUTH` or `UP`.
- Both cells build the same key, so they always get the same answer. The four edges of the spawn cell are always open.

### Region maze

The maze guarantees that every cell can be reached from spawn.

1. Regions are 8 × 8 cells: `rx = floorDiv(cx, 8)` and `rz = floorDiv(cz, 8)`.
2. For each floor of a region, build a random spanning tree over its 64 cells, seeded by `hash(seed, rx, rz, f)`. Use randomised Kruskal or an iterative depth-first search. Tree edges are open.
3. Each floor of a region opens 2 doors into the region to its east and 2 into the region to its south. Pick the cells with `hash(seed, boundary, f)`, so both regions choose the same ones.
4. Between floors `f` and `f + 1`, each region opens 2 vertical edges at distinct cells. Skip the spawn chunk, the portal chunk, and cells already holding stairs to another floor. These become staircases.
5. Every other horizontal edge opens with probability 0.15, from `hash(seed, edge)`. This adds the loops.

Each floor of each region is connected (step 2), touches its neighbours (step 3) and links to the next floor (step 4). So the whole stronghold is one connected piece, however far it goes.

### Room choice

- Fixed cells come first: the spawn cell `(0, 0, 0)`, each region's portal cell, and the stair cells.
- Every other cell picks from the Room catalog by weight, seeded by `hash(seed, cx, cz, f, "room")`, among rooms whose door rules fit its open sides. If nothing fits, it is a plain hall.
- Rooms are written in one orientation and rotated about (8, 8) to face their open sides.

### Determinism

- All randomness comes from a stateless hash of the world seed, the coordinates and a salt, such as SplitMix64. Never use a shared `Random`, and never depend on the order chunks generate in.
- The same seed gives the same stronghold, chunk for chunk, in every world and after every restart.
- Cache finished region mazes in a small thread-safe cache. Rebuilding one is cheap: a few hundred edges.

## Room catalog

Every room fits inside the 15 × 15 room area and 15 blocks of height, and has doorways only on the cell's open sides. Shapes follow vanilla's stronghold pieces, adapted to that space; read `StrongholdPieces` in 26.3 for block-level detail.

| Room | Fits cells with | What's inside | Weight |
|---|---|---|---|
| Hall | Any open sides | 5-wide halls from each open side to the centre; a wall torch now and then | 50 |
| Chest hall | 2 opposite open sides | Straight hall with a chest in a wall niche (corridor loot) | 8 |
| Pillar room | Any open sides | 11 × 11 room, 7 high, with a central stone brick pillar ringed by torches | 8 |
| Fountain room | Any open sides | 11 × 11 room with a raised fountain holding a water source | 5 |
| Storeroom | Any open sides | Two-level room with an oak plank balcony, a ladder and a chest (crossing loot) | 5 |
| Prison hall | 2 opposite open sides | Hall with 4 cells behind iron bars and iron doors | 6 |
| Grand crossing | 3 or 4 open sides | Tall junction with a raised walkway, like vanilla's five-way crossing | 6 |
| Library | 1 or 2 open sides | Bookshelf walls and cobwebs; a tall version adds a balcony and chandelier; 1 or 2 chests (library loot) | 4 |
| Stairs | Both ends of a vertical edge | The lower cell holds a spiral stair; the upper cell is a small hall the stair rises into | Fixed |
| Portal room | The region's portal cell | End portal room; see Portal rooms | Fixed |
| Spawn room | The spawn cell | Pillar room lit with torches; no infested bricks, no spawner | Fixed |

Weights are relative. Tune them after the first playtest.

## Blocks, loot and mobs

Use vanilla stronghold materials, choosing each block with a position hash so the result is deterministic and thread-safe.

Blocks:

- Walls, floors and ceilings: mostly stone bricks, mixed with cracked, mossy and some infested stone bricks in the proportions of vanilla's stronghold block selector.
- Trim: stone brick stairs and slabs, smooth stone slabs, iron bars, oak planks, oak fences, ladders, bookshelves, cobwebs and wall torches.
- Doorways, chosen per edge and built in the connector: an open arch, an oak door in a stone brick frame, an iron bars grate, or an iron door with stone buttons on both sides. Copy the shapes from vanilla's small door types.
- No infested bricks in the spawn room or its connectors.

Loot:

| Chests in | Loot table | Rolls |
|---|---|---|
| Chest halls | `endless_stronghold:chests/corridor` | `minecraft:chests/stronghold_corridor` plus the extra pool |
| Storerooms | `endless_stronghold:chests/crossing` | `minecraft:chests/stronghold_crossing` plus the extra pool |
| Libraries | `endless_stronghold:chests/library` | `minecraft:chests/stronghold_library` plus the extra pool |

- Each mod table has 2 pools: one `minecraft:loot_table` entry pointing at the vanilla table, and the extra pool. Check the 26.3 loot table format, because 26.3 reworked number providers and reloadable registries.
- Extra pool: a 25% chance of 1 to 3 flint, and a 5% chance of 1 eye of ender.
- Chests store their loot table and a seed when generated. Items roll the first time a player opens them, as in vanilla.

Mobs:

- The biome's spawn list is the monster list from `minecraft:plains`, with no animals.
- Monsters spawn only in darkness, so the lit spawn room stays safe.
- Each portal room has a silverfish spawner on its stairs, as in vanilla.

## Portal rooms, eyes of ender and spawn

Portal rooms are a registered structure, so eyes of ender and `/locate` find them through vanilla code with no mixins. The spawn point is set explicitly, because vanilla's spawn search would put players on the roof.

### Portal rooms

1. Register a structure type and a structure piece type for `endless_stronghold:portal_room`.
2. The structure set `endless_stronghold:portal_rooms` uses `minecraft:random_spread` placement with spacing 8, separation 2 and its own salt. Spacing 8 equals the region size, so vanilla picks exactly one candidate chunk per region.
3. The layout service asks that same placement for each region's chunk (in 1.21 this was `RandomSpreadStructurePlacement#getPotentialStructureChunk`). The portal's floor is `hash(seed, rx, rz, "portal") mod 4`, and the generator leaves that cell empty for the structure.
4. The structure's generation point comes straight from the layout, never from terrain heights, so locating works for chunks that don't exist yet.
5. The structure piece builds the room in the decoration stage. It asks the layout service which sides are open, so its doorways match the maze.
6. Add the structure to `#minecraft:eye_of_ender_located`. Thrown eyes then fly to the nearest portal room, and `/locate structure endless_stronghold:portal_room` works.
7. If region (0, 0) picks the spawn chunk, its portal goes on a floor other than floor 1.

Inside, adapted from vanilla's portal room to fit 11 × 15:

- 12 end portal frames around a 3 × 3 portal, each with a 10% chance of a pre-placed eye. The portal lights only when all 12 frames hold eyes.
- A raised platform reached by stairs, lava under the portal, iron bar windows, and a silverfish spawner on the stairs.

Fallback: if the structure route fails in 26.3, a small mixin on the eye of ender's locate call can ask the layout service for the nearest portal cell instead.

### Spawn

- The spawn cell is chunk (0, 0) on floor 1. All four of its edges are open, and it never holds stairs.
- When a new world of this type first loads, set the world spawn to the middle of the spawn room's floor.
- Set the spawn radius game rule to 0 for these worlds, checking its exact name in 26.3.
- Players who die without a bed then return to the spawn room.

## Code structure

Keep the layout maths in plain Java with no Minecraft imports, and keep the Minecraft-facing classes as thin wrappers around it.

| Component | Job | Minecraft hook |
|---|---|---|
| `EndlessStronghold` | Registers the generator codec, structure type and piece type; sets up spawn | Mod initializer, plus a server-start event |
| `EndlessStrongholdGenerator` | Builds every cell, on every floor, of each chunk | Extends `ChunkGenerator`; model it on vanilla `FlatLevelSource` |
| `GeneratorSettings` | Floors, base Y, region size and loop chance, with the defaults above | Codec read from the world preset JSON |
| `layout.LayoutService` | Edges, region mazes, room choice, and the stair, portal and spawn cells | None |
| `layout.RegionMaze` | Spanning trees, boundary doors, stairs and loops for one region | None |
| `layout.Hash` | Stateless mixing of seed, coordinates and salt | None |
| `rooms.RoomBuilder` and one class per room | Writes one room into a cell, given its open sides and rotation | Through `CellWriter` |
| `rooms.CellWriter` | Converts local to world coordinates and rotates about (8, 8); wraps `ChunkAccess` or `WorldGenLevel` | Both generation stages |
| `structure.PortalRoomStructure` and `PortalRoomPiece` | The locatable portal room | Structure type and piece type registries |

### Generation stages

- `fillFromNoise`: every plain block for the chunk's cells and its west and north connectors. Write straight into the chunk without neighbour updates, and update the generation heightmaps the way `FlatLevelSource` does.
- `applyBiomeDecoration`: call the parent method first, so structure pieces such as the portal room get placed. Then place blocks that need block entities: chests (loot table and seed) and spawners.
- `buildSurface`, `applyCarvers` and `spawnOriginalMobs` do nothing. `getBaseHeight` and `getBaseColumn` describe the generated blocks if that's cheap, or an empty column otherwise.

### Threading and speed

- Several threads generate chunks at once. Keep no static mutable state, and make every cache thread-safe and bounded.
- Don't allocate per block; reuse one `BlockPos.MutableBlockPos` per call.
- Target: a chunk of this mod generates faster than a vanilla Overworld chunk.

## Testing and acceptance criteria

A phase is done when its unit tests pass, the CI build is green, and the player checklist items for that phase pass.

### Unit tests (JUnit, layout code only)

- Edge agreement: for 10,000 random edges, both cells give the same answer.
- Connectivity: a breadth-first search from the spawn cell over 5 × 5 regions (40 × 40 cells, 4 floors) reaches every cell.
- Determinism: one seed gives the same layout twice, and two seeds give different layouts.
- Portal rooms: exactly one per region, never in the spawn cell.
- Stairs: 2 per pair of floors in each region, in distinct cells.
- Rooms: each builder writes only inside local 0..15 and its floor's band, and makes doorways only on open sides.

### In-game smoke test

Run a Fabric game test, or a headless dev server in CI, that:

- Generates every chunk within 256 blocks of spawn without errors.
- Finds a solid floor with two air blocks above it at the spawn point.
- Finds a walkable passage through every open edge.

### CI

GitHub Actions runs `./gradlew build` on JDK 25 for every push and uploads the jar as an artifact.

### Player checklist

1. Create a world with World Type set to Endless Stronghold. You spawn inside a lit room.
2. Walk 300 blocks north, south, east and west. No door leads into a wall, and no hole opens onto the void.
3. Reach all 4 floors by the stairs.
4. In spectator mode, fly outside. There is only the stronghold, floating in the void.
5. Throw an eye of ender. It flies toward a portal room (Phase 3).
6. Quit and reload: everything matches. A new world with the same seed has the same layout.
7. Flying around at render distance 12 doesn't stall chunk loading.

## Build plan

Four phases, each ending with a jar you can test in the game.

1. **Phase 1: walkable skeleton.** The project from the 26.3 example mod, the CI workflow, the world preset and biome, and a generator that builds one floor of plain halls with every edge open and open arches only. Includes the spawn room and the spawn fix.
   Done when: a new Endless Stronghold world spawns you inside, and halls run on in every direction (checklist 1, 2 and 6).
2. **Phase 2: the real stronghold.** Region mazes, loops, 4 floors with stairs, every room in the catalog except the portal room, the block palette, door types, chests with loot, and mob spawning.
   Done when: all unit tests pass, and checklist items 1–4, 6 and 7 pass.
3. **Phase 3: portal rooms.** The portal room structure, structure set and eye of ender tag, silverfish spawners, and the extra loot pool.
   Done when: an eye of ender leads to a portal room, and the End can be reached (checklist 5).
4. **Phase 4: polish.** Generator settings exposed in the preset JSON, a performance pass, a README with screenshots, and release metadata for Modrinth.
   Done when: the full checklist passes on a fresh world.

## Risks and gotchas

The likeliest failures are outdated API names and doorways that miss by one block. Each item below names its guard.

- **Old API names.** Most modding examples online predate 26.1 and use Yarn names. Check every name against the 26.3 sources.
- **Skipping the parent decoration call.** If `applyBiomeDecoration` doesn't call the parent method, the portal room structure never gets placed.
- **Doors one block off.** Rotate only about (8, 8) and keep every doorway at 7..9. Flipping across the whole chunk (x → 15 - x) shifts doors by one block.
- **Writing outside the chunk.** `fillFromNoise` may only touch its own chunk. That's why each chunk builds only its west and north connectors.
- **Block entities placed too early.** Chests and spawners go in the decoration stage, not in `fillFromNoise`.
- **Spawning on the roof.** Vanilla's spawn search picks the highest block. Set the spawn explicitly and the spawn radius to 0.
- **Portal rooms in the wrong cells.** If the structure set's spacing ever differs from the region size, the portal rooms and the maze disagree.
- **Hidden randomness.** A shared `Random`, or logic that depends on generation order, causes seams that show up only on some runs.
- **Blocked downloads in Claude Code.** If the cloud environment can't reach Fabric's and Mojang's servers, local builds fail. Allow the domains listed under How to hand this to Claude Code, or rely on CI.

## Installing and playing

Install Fabric for 26.3, put two jars in the mods folder, and pick the new world type.

1. Install Fabric Loader for Minecraft 26.3 with the installer from fabricmc.net.
2. Download Fabric API for 26.3, and the Endless Stronghold jar from the GitHub Actions run or a release. An Actions download comes as a .zip; unzip it to get the jar.
3. Put both jars in the `mods` folder inside your Minecraft folder.
4. Start the Fabric 26.3 profile and choose Create New World. On the World tab, set World Type to Endless Stronghold.
5. For a server, put both jars in the server's `mods` folder and set `level-type=endless_stronghold:endless_stronghold` before the world is created. Players joining shouldn't need the mod, since the world is made of vanilla blocks; confirm this in Phase 1.

## Sources

- Fabric for Minecraft 26.3, Fabric blog, 15 September 2026
- Fabric for Minecraft 26.1, Fabric blog, 14 March 2026
- Minecraft 26.3: The New Wilderness Bound, Cybrancee
- Configure cloud environments, Claude Code docs
