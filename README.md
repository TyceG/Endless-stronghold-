# Endless Stronghold

A Fabric mod for Minecraft Java Edition 26.3 that adds a world type where the Overworld is one stronghold, floating in the void, that never ends.

The full plan is in [DESIGN.md](DESIGN.md); changes to it are in [DECISIONS.md](DECISIONS.md).

## Status

**Phase 1 of 4: walkable skeleton.** You spawn in a lit pillar room, and plain stone brick halls run on in every direction, on one floor. Region mazes, the other floors, the room catalog, loot and portal rooms come in later phases.

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

The jar lands in `build/libs/`. `./gradlew test` runs the layout unit tests.

### Smoke test

`./gradlew runSmokeServer` starts a headless 26.3 server in `run/smoke`, creates an Endless Stronghold world, generates every chunk within 256 blocks of spawn, checks the spawn point and every doorway, logs `[smoke] PASS` or `[smoke] FAIL`, and stops. Before the first run, create `run/smoke/eula.txt` containing `eula=true` (this accepts the [Minecraft EULA](https://www.minecraft.net/eula)) and `run/smoke/server.properties` containing `level-type=endless_stronghold\:endless_stronghold`. Delete `run/smoke/world` to test a fresh world.

## License

CC0 1.0, as in the Fabric example mod.
