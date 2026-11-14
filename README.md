# Endless Stronghold

A Fabric mod for Minecraft Java Edition 26.3 that adds a world type where the Overworld is one stronghold, floating in the void, that never ends.

The full plan is in [DESIGN.md](DESIGN.md); changes to it are in [DECISIONS.md](DECISIONS.md).

## Status

**Phase 2 of 4: the real stronghold.** Four floors, 12 blocks apart, a maze in every 128 × 128 block area, joined by vanilla's spiral staircases. Every room is a vanilla stronghold piece, block for block: libraries, prison halls, chest corridors, five-way crossings, and pillar, fountain, storeroom and empty room crossings, joined by vanilla-style corridors, with vanilla's doors, cracked, mossy and infested bricks, and chests with stronghold loot. Portal rooms, eyes of ender and the extra loot come in Phase 3.

## Playing it

1. Install Fabric Loader for Minecraft 26.3 from [fabricmc.net](https://fabricmc.net/use/installer/).
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for 26.3.
3. Get the Endless Stronghold jar: open the newest run on the [Actions tab](https://github.com/TyceG/Endless-stronghold-/actions), download **Artifacts** at the bottom, and unzip it. Use `endless-stronghold-<version>.jar`, not the `-sources` one.
4. Put both jars in the `mods` folder of your Minecraft folder.
5. Start the Fabric 26.3 profile, choose **Create New World**, and on the **World** tab set **World Type** to **Endless Stronghold**.

For a dedicated server, put both jars in its `mods` folder and set `level-type=endless_stronghold:endless_stronghold` in `server.properties` before the world is first created.

## Building

Needs JDK 25.

```
./gradlew build
```

The jar lands in `build/libs/`. `./gradlew test` runs the unit tests: the layout, every room, and a walk through 16 × 16 chunks on all floors.

### Smoke test

`./gradlew runSmokeServer` starts a headless 26.3 server in `run/smoke`, creates an Endless Stronghold world, generates every chunk within 256 blocks of spawn, checks the spawn point and every doorway on every floor, opens a chest of each loot table, logs `[smoke] PASS` or `[smoke] FAIL`, and stops. Before the first run, create `run/smoke/eula.txt` containing `eula=true` (this accepts the [Minecraft EULA](https://www.minecraft.net/eula)) and `run/smoke/server.properties` containing `level-type=endless_stronghold\:endless_stronghold`. Delete `run/smoke/world` to test a fresh world.

## License

CC0 1.0, as in the Fabric example mod.
