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

**Vanilla's room weights, not the doc's.** Tyce asked for everything to generate as close to vanilla as possible, so room weights come from vanilla's `StrongholdPieces` piece weights. Plain hall 80 (vanilla Straight 40 + Left turn 20 + Right turn 20), Library 10, Chest corridor 5, Prison hall 5, Five-way crossing 5, and vanilla's Room crossing (10) split by its own type roll: Pillar room 2, Fountain room 2, Storeroom 2, and **Plain room 4**. The plain, empty room is vanilla's room crossing types 3 and 4; the doc's catalog didn't list it. Vanilla's per-stronghold limits (such as at most 2 libraries) can't apply to an endless stronghold and are left out.

**Region mazes use randomised Kruskal.** Each floor of each region is a uniform random spanning tree, which gives vanilla-like branching with dead ends. Region-boundary doors, stairs and loops follow the doc.

**Doors are vanilla's small door types, one per edge**, with vanilla's odds: 2 in 5 open arches, 1 in 5 each oak door, iron bar grate and iron door. Iron doors get a stone button on each side. The far-side button sits in the neighbouring chunk, so that chunk places it: each cell adds the buttons for iron doors on its east and south edges.

**Inside air is cave air**, as in vanilla strongholds.

**Chests are placed in the decoration stage** by running the room builders a second time and keeping only the chests, each with its loot table and a seed from a position hash. Neighbour-dependent blocks (iron bars, fences, ladders, torches) and the fountain's water are marked for vanilla's post-processing, as vanilla's structure pieces do, so bars and fences join up and water flows.

**Loot tables nest vanilla's.** `endless_stronghold:chests/corridor`, `crossing` and `library` each hold one `minecraft:loot_table` entry pointing at vanilla's stronghold table. The extra pool (flint, eye of ender) arrives in Phase 3, as the build plan says.

**Portal cells come in Phase 3.** The doc keeps stairs out of each region's portal chunk, but the portal chunk comes from the structure placement added in Phase 3. Until then, stairs can land anywhere but the spawn chunk, so in Phase 3 some regions' stairs will move. Worlds made with Phase 2 will change shape in those regions when they upgrade.

**Region size and loop chance aren't settings yet.** They're constants (8 and 0.15) until Phase 4, because the region size has to match the portal structure's spacing.

**Extra tests.** Besides the doc's unit tests, a walking test builds 16 × 16 chunks on all 4 floors and walks them as a player would: stepping up stairs and slabs with headroom, climbing ladders, through doors. It checks that every cell the maze connects can be reached on foot from spawn.

## Phase 2, second pass (after the first playtest)

Tyce's playtest: the library sometimes had two doorways, and the library, prison hall, staircases and "grand crossing" weren't vanilla's. Every room must be exactly the real thing. So rooms are now vanilla's `StrongholdPieces`, block for block, and the layout changed to make room for them.

**Rooms are placed with vanilla's own transform.** `rooms.Piece` copies `StructurePiece`'s orientation exactly: NORTH flips z, SOUTH mirrors block states north-south, EAST and WEST swap x and z, and states are mirrored then rotated like vanilla's `placeBlock`. Vanilla writes its piece code for this transform (a pillar room's north torch is written facing south, and comes out right once transformed), so the piece code is copied line for line. The first pass had turned two pillar-room torches the wrong way; this fixes them.

**Floors are 12 blocks apart, not 16.** Vanilla's stair pieces each go down exactly 6 blocks, so two of them join floors 12 apart. Floors are now at Y 0, 12, 24 and 36; the spawn point is (8, 13, 5).

**Staircases are two of vanilla's spiral StairsDown pieces.** The upper one comes down from the upper floor to a corridor halfway between the floors; that corridor leads around the cell to the lower one, which comes down to the lower floor. Staircases only go in cells where the upper floor has a closed side for the upper piece and the lower floor has a different closed side for the lower one (`layout.StairPlan`). Vanilla's straight staircase came in the third pass, below.

| Room | How it fits |
|---|---|
| Library | Vanilla's 14 x 15, 6 or 11 high, with one door, like vanilla. Only in dead-end cells. It's as big as the cell, so it uses the closed connector strip on one side; entered from the north or west its front wall is the cell's own connector. Its door is off-centre (vanilla's is 4 blocks from one side), so the hall leading to it bends to meet the door, and the edge's doorway sits where the door is. Tall or short is still a 50/50 roll. |
| Prison hall | Vanilla's piece exactly: 2 cells on one side. The first pass's mirrored 4-cell version is gone. Straight cells only, with short corridors at each end. |
| Chest corridor | Vanilla's piece exactly, in place of the first pass's hall with a niche. Straight cells only. |
| Room crossings | Vanilla's piece exactly. Vanilla always cuts openings on all four sides; here only on open sides, since a closed side would open onto the void. |
| Five-way crossing | Replaces the "grand crossing", which wasn't vanilla. Vanilla's piece exactly, sunk 2 blocks so its front door and low side exits are at the floor. See the third pass for where it goes and which exits it has. Never over a tall library (it sinks into the floor below). |
| Spawn room | Vanilla's pillar room exactly, plus 8 wall torches and no infested bricks. |

**Chests turn like vanilla's**: they're placed with vanilla's `StructurePiece.reorient`, facing away from a single solid neighbour.

**Each piece has its own front door**, a random small door like vanilla's `entryDoor`. Some rooms have a door in their own wall a few blocks behind the connector's door, as vanilla pieces do.

**Iron doors in front of a library entered from the south or east become oak doors.** Their far button would have to sit in the library's wall.

**Halls bent toward a library have no wall torches**, because the bend cuts through the walls they'd hang on.

## Phase 2, third pass (after the second playtest)

**Five-way crossings only at T-junctions, with no high exits.** Tyce found its doorways led to dead ends everywhere. Its high side exits are 2 blocks above any floor, so they led into short closed corridors. Vanilla rolls them at random (50% and 67%), and "neither" is one of vanilla's possible rolls, so they're left out. The room now goes only where its front door and both low side exits lead into the maze: entered from the south with east and west open and the north closed, or from the east with north and south open and the west closed. The far exit still can't connect (it's 2 blocks below the floor and would open onto the next chunk) and stays walled.

**Straight cobblestone staircases.** Vanilla weighs its spiral (StairsDown) and straight (StraightStairsDown) pieces equally, so now one of each region's two staircases per pair of floors is straight. The straight piece is 8 long, and two of them end to end are exactly 16, the width of a chunk. So a straight staircase runs across a whole cell: in from the upper floor on one side, down one piece, straight into the second, down again, and out the opposite side on the lower floor, chained the way vanilla chains them. That needs a cell that's a dead end on both floors, facing opposite ways. The maze makes one per region per pair of floors: the cell is chosen first (inside the region, away from spawn), its trees attach it by that one edge, and no extra openings are added around it.

Where a straight staircase's piece meets its own north or west connector, the piece's wall is the connector: the top piece's front wall holds the edge's door, and the bottom piece's exit is vanilla's opening. Where it meets the south or east edge, the neighbour's connector holds the door; an iron door there becomes an oak door, since its button would have to sit in the piece's wall.

## Phase 2, fourth pass: grown like vanilla

Tyce asked how vanilla makes all five of the five-way crossing's doorways work, and to do it the same way. Vanilla has no floors or grid: it grows a stronghold as a tree of pieces, each new piece attached at a doorway's exact position and height. So the floor-and-maze layout from the design doc (cells, region mazes, 4 floors, stair cells) is replaced by vanilla's own algorithm. This supersedes the layout parts of the earlier passes; the rooms are unchanged, still vanilla's code block for block.

**How a region grows** (`gen.Grower`), as vanilla's `StrongholdPieces` does: a list of pending pieces is worked through in random order; each piece tries to attach a child at each of its doorways (forward, left, right, at their real heights), rolling the kind with vanilla's weights (Straight 40, Prison hall 5, Left turn 20, Right turn 20, Room crossing 10, Straight stairs 5, Spiral stairs 5, Five-way crossing 5, Chest corridor 5, Library 10, Portal room 20), vanilla's per-stronghold limits (5, 6, 5, 5, 4, 4, 2 and 1), five tries, never the same kind twice in a row, libraries only 5+ pieces deep and the portal room 6+, a depth limit of 50, no overlaps, and the lowest height vanilla allows (54 below the start). When nothing fits, vanilla's filler corridor runs up to the piece in the way, breaking one block into it, as vanilla's does. Libraries are tall when they fit. Every piece's random properties (its door, side exits, room type, high and low exits) are rolled as vanilla's constructors roll them. Vanilla's quirks come along too: a five-way crossing facing north or west attaches its side children at the wrong heights, as in vanilla.

**Endless.** Vanilla grows one stronghold within 112 blocks of its start. Here the world is split into 256 x 256 regions (16 x 16 chunks, region (0, 0) centred on the origin), and each grows its own stronghold, staying inside the region instead of within 112 blocks. To make them one endless stronghold, each region starts not from vanilla's single spiral staircase but from its main corridors: a room crossing at the centre (floor at Y 30) and a line of vanilla's straight-through pieces (straight, room crossing, prison hall, chest corridor, both staircases, rolled with vanilla's weights) from it to the middle of each edge. The line is exactly as long as it needs to be to reach the edge, with 0, 1 or 2 staircases (fixed per edge), so it meets the neighbouring region's line in line and level. Everything else grows from the main corridors, as vanilla grows from its start, and the main corridors' own side doorways grow branches.

**Every region has its portal room.** Vanilla starts over until its stronghold has one; each region does too (up to 20 tries). Eyes of ender and `/locate` come in Phase 3; the portal room and its silverfish spawner are built now.

**Doorways that lead nowhere get stone.** Underground, a vanilla doorway with nothing beyond it looks onto stone. In the void it would look onto nothing, so each block just outside such a doorway that no piece covers is stone, where vanilla's would be. A main corridor's last doorway meets the next region's and stays open.

**Spawn** is in the spawn region's hub, a pillar room (vanilla's type 0) with 8 extra wall torches and no infested bricks: (0, 31, -3).

**Each chunk builds the pieces of its region that reach into it**, clipped to the chunk, as vanilla builds a structure's pieces chunk by chunk. Regions line up with chunks, so a chunk belongs to exactly one. Each piece draws its own randomness (block mix, torches, cobwebs, portal eyes) from its own seed, so a piece crossing chunk borders comes out the same in each.

**Tests.** A walk from spawn across two whole regions reaches every main corridor piece, the next region's hub, and 98 to 99% of all pieces (the rest sit behind vanilla's misplaced five-way children). No air touches the void. Every region keeps vanilla's limits and has one portal room, and every pair of neighbouring regions' main corridors meet.

## DESIGN.md revised

After Phase 2, `DESIGN.md` was rewritten to describe the stronghold as it is now built (vanilla growth, regions, main corridors) instead of the original floors and maze, and its Phase 3 plan for eyes of ender changed: the generator answers `findNearestMapStructure` itself from the region plans, instead of a `random_spread` structure set, which could not match where the growth puts each portal room. The original version is in git history.
