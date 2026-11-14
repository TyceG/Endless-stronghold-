# Decisions

Changes of plan from [DESIGN.md](DESIGN.md), newest last.

## Phase 1

**Newer toolchain than the doc lists.** The official Fabric example mod for 26.3 now uses Gradle 9.8.2, and the newest stable Loom is 1.19.4, so the project uses those instead of Loom 1.17 and Gradle 9.6.0. Fabric Loader 0.20.1 and Fabric API 0.166.2+26.3 are the newest 26.3 builds on fabricmc.net/develop. Both are "or newer" in the doc.

**26.3 names that differ from the doc:**

| Doc says | 26.3 has |
|---|---|
| `fillFromNoise` | `buildTerrain`, with two extra parameters (a nullable `WorldGenRegion` and the set of possible biomes) |
| `buildSurface`, `applyCarvers` | Gone from `ChunkGenerator`; nothing to override |
| spawn radius game rule | `GameRules.RESPAWN_RADIUS`, id `respawn_radius` |
| `ResourceLocation` | `Identifier` |
| biome `spawners` / `spawn_costs` | Now the `minecraft:gameplay/natural_mob_spawns` attribute |

**Spawn uses two mixins, not a server-start event.** Vanilla picks a new world's spawn in a private `MinecraftServer.setInitialSpawn`, which searches 11 × 11 chunks for the highest block. Setting the spawn afterwards from a server-start event would also have to tell a fresh world from a reloaded one, and the bonus chest would already have landed on the roof. So:

- `MinecraftServerMixin` replaces `setInitialSpawn` for Endless Stronghold worlds only: spawn goes to the spawn room and `respawn_radius` is set to 0. It runs once, when the world is created.
- `PlayerSpawnFinderMixin`: even with `respawn_radius` 0, vanilla's `PlayerSpawnFinder.findSpawn` moves joining and respawning players to the top of the spawn column, which is the roof. For Endless Stronghold worlds it now keeps them at the world spawn, the way vanilla already does in adventure mode. Beds are unaffected. It loads the spawn chunk before measuring the floor, like vanilla's own search: in the first playtest it didn't, so a player joining before the spawn area had loaded found no floor and was dropped to Y -63, into the void. The smoke test now checks a spawn point in an unloaded chunk.

**No bonus chest.** The Bonus Chest world option does nothing in Endless Stronghold worlds, because the spawn room replaces vanilla's spawn setup.

**Spawn point is in front of the pillar.** The spawn room has a central pillar, so "the middle of the spawn room's floor" is inside it. Players spawn at local (8, 1, 5): on the floor, between the north doorway and the pillar, which is world (8, 17, 5).

**The spawn cell is on floor 1.** The doc says floor 1 under Decisions and Spawn, but `(0, 0, 0)` under Room choice. Floor 1 is used everywhere.

**Phase 1 builds floor 1 only.** The spawn cell is on floor 1, so Phase 1's one floor of halls is floor 1 (floor blocks at Y 16). Floors 0, 2 and 3 arrive with the stairs in Phase 2. `GeneratorSettings` already reads `floors` and `base_y` from the preset, both optional, but the preset doesn't set them yet (Phase 4).

**Room builders write abstract blocks.** So the rooms can be unit-tested without Minecraft, they write a small `rooms.Block` type, which `world.ChunkCellWriter` maps to block states. `CellWriter` itself is plain Java and does the rotation about (8, 8).

**Smoke test is a dev-server run, not CI.** `./gradlew runSmokeServer` starts a headless server that builds a fresh world, checks it and stops. It only runs with `-Dendless_stronghold.smoke=true`, which that task sets. It needs `run/smoke/eula.txt` with `eula=true` (Mojang's EULA), so it isn't in CI.

## Phase 2

**Vanilla's room weights, not the doc's.** Tyce asked for everything to generate as close to vanilla as possible, so room weights come from vanilla's `StrongholdPieces` piece weights. Plain hall 80 (vanilla Straight 40 + Left turn 20 + Right turn 20), Library 10, Chest hall 5, Prison hall 5, Grand crossing 5 (vanilla Five-way crossing), and vanilla's Room crossing (10) split by its own type roll: Pillar room 2, Fountain room 2, Storeroom 2, and **Plain room 4**. The plain, empty room is vanilla's room crossing types 3 and 4; the doc's catalog didn't list it. Vanilla's per-stronghold limits (such as at most 2 libraries) can't apply to an endless stronghold and are left out.

**Rooms are vanilla's pieces, block for block where they fit.** Coordinates are offset to centre each piece on block (8, 8). Changes where vanilla's piece doesn't fit a cell:

| Room | Change from vanilla |
|---|---|
| Spawn room | Now vanilla's pillar room (room crossing type 0) with 8 extra wall torches, instead of Phase 1's own design. Spawn point unchanged: (8, 17, 5). |
| Library | 13 × 13 instead of 14 × 15, so every doorway gets a hall in front of it. Two pairs of free-standing shelves instead of three, leaving 3-wide aisles down the middle and across, so a doorway on any side reaches the room. Tall or short is a 50/50 roll (vanilla picks tall when it has room). |
| Prison hall | Vanilla's piece has 2 cells on one side; it's mirrored to give 4, as the doc asks. |
| Grand crossing | Vanilla's five-way crossing has exits at three heights. Every doorway here is at floor level, and the raised walkway (double smooth stone slabs on stone brick pillars, with a ladder up) passes over them. |
| Stairs | Vanilla's stair pieces drop only 6 blocks; floors are 16 apart. New: a spiral of stone brick stairs around a pillar, one block up per step, with the top cell's floor open over the last 3 steps (2 isn't enough headroom: the walking test caught it). |
| Halls | Torches like vanilla's Straight piece: a 1 in 10 chance at each of 2 spots per wall stretch. |

**Region mazes use randomised Kruskal.** Each floor of each region is a uniform random spanning tree, which gives vanilla-like branching with dead ends. Region-boundary doors, stairs and loops follow the doc.

**Doors are vanilla's small door types, one per edge**, with vanilla's odds: 2 in 5 open arches, 1 in 5 each oak door, iron bar grate and iron door. Iron doors get a stone button on each side. The far-side button sits in the neighbouring chunk, so that chunk places it: each cell adds the buttons for iron doors on its east and south edges.

**Inside air is cave air**, as in vanilla strongholds.

**Chests are placed in the decoration stage** by running the room builders a second time and keeping only the chests, each with its loot table and a seed from a position hash. Neighbour-dependent blocks (iron bars, fences, ladders, torches) and the fountain's water are marked for vanilla's post-processing, as vanilla's structure pieces do, so bars and fences join up and water flows.

**Loot tables nest vanilla's.** `endless_stronghold:chests/corridor`, `crossing` and `library` each hold one `minecraft:loot_table` entry pointing at vanilla's stronghold table. The extra pool (flint, eye of ender) arrives in Phase 3, as the build plan says.

**Portal cells come in Phase 3.** The doc keeps stairs out of each region's portal chunk, but the portal chunk comes from the structure placement added in Phase 3. Until then, stairs can land anywhere but the spawn chunk, so in Phase 3 some regions' stairs will move. Worlds made with Phase 2 will change shape in those regions when they upgrade.

**Region size and loop chance aren't settings yet.** They're constants (8 and 0.15) until Phase 4, because the region size has to match the portal structure's spacing.

**Extra tests.** Besides the doc's unit tests, a walking test builds 16 × 16 chunks on all 4 floors and walks them as a player would: stepping up stairs and slabs with headroom, climbing ladders, through doors. It checks that every cell the maze connects can be reached on foot from spawn.
