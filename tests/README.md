# Tests (development only)

This folder is not a mod. It is the development-only test mod of the J's Tech Series: the GameTests, the
client tests, the series' own helpers for them and the debug commands for every mod of the series, kept out of
the shipped jars. It is built on J's Core Test Kit ([testkit/](../testkit/README.md)), as any mod's tests would be.

- It is never released. No release, and no page anywhere, will ever offer it.
- It adds nothing to the game and must not be installed. A jar built from it is a by-product of the build,
  named `jstests-<minecraft version>-<version>-development-only.jar`, and only exists so the development
  runs can load the tests beside the mods.
- If you found this jar in a modpack or a download, it does not belong there.

For contributors: `./gradlew runGameTestServer` runs every GameTest, `./gradlew runClientTests0` runs a
client test shard, and `./gradlew runClient` starts the game with the whole series and this mod loaded. The client
tests run in a flat world; `-PctWorld=normal -PctSeed=<seed>` runs those asked for with `-PctOnly` in the game's
normal land of that seed instead, for a look at how the land itself is drawn from far above.
