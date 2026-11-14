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
- `PlayerSpawnFinderMixin`: even with `respawn_radius` 0, vanilla's `PlayerSpawnFinder.findSpawn` moves joining and respawning players to the top of the spawn column, which is the roof. For Endless Stronghold worlds it now keeps them at the world spawn, the way vanilla already does in adventure mode. Beds are unaffected.

**No bonus chest.** The Bonus Chest world option does nothing in Endless Stronghold worlds, because the spawn room replaces vanilla's spawn setup.

**Spawn point is in front of the pillar.** The spawn room has a central pillar, so "the middle of the spawn room's floor" is inside it. Players spawn at local (8, 1, 5): on the floor, between the north doorway and the pillar, which is world (8, 17, 5).

**The spawn cell is on floor 1.** The doc says floor 1 under Decisions and Spawn, but `(0, 0, 0)` under Room choice. Floor 1 is used everywhere.

**Phase 1 builds floor 1 only.** The spawn cell is on floor 1, so Phase 1's one floor of halls is floor 1 (floor blocks at Y 16). Floors 0, 2 and 3 arrive with the stairs in Phase 2. `GeneratorSettings` already reads `floors` and `base_y` from the preset, both optional, but the preset doesn't set them yet (Phase 4).

**Room builders write abstract blocks.** So the rooms can be unit-tested without Minecraft, they write a small `rooms.Block` enum, which `world.ChunkCellWriter` maps to block states. `CellWriter` itself is plain Java and does the rotation about (8, 8).

**Smoke test is a dev-server run, not CI.** `./gradlew runSmokeServer` starts a headless server that builds a fresh world, checks it and stops. It only runs with `-Dendless_stronghold.smoke=true`, which that task sets. It needs `run/smoke/eula.txt` with `eula=true` (Mojang's EULA), so it isn't in CI.
