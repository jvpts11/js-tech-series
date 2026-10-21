# J's Core Test Kit

What a mod built on J's Core writes its tests with: a mod of its own, `jstestkit`, that loads beside the mod it
tests in the GameTest server and in the client test windows. It adds nothing to the game, is never installed and
never released: it is a test dependency only.

- **GameTests**: arenas made in code, a scene for every kit of the Core (the data network and its Operations,
  energy, machines and their recipes, multiblocks, sealed rooms, moving structures, hazards, menus), round trips
  through the save, time and chance under control, assertions that explain, tests of cost, old saves, optional
  mods seen absent, players who log in, and a report of time and a stall monitor for long runs.
- **Client tests**: the runner that plays the game in a window, with its helper, reference pictures and shards
  balanced by how long each test took.
- **Checks for unit tests**: the test of the API's surface, and the data generation kept up to date.

For now it is published only to the local Maven repository: `./gradlew :testkit:publishToMavenLocal`, then
`dev.jstech:testkit:<version>` from `mavenLocal()`.

How to use it: [core/docs/TESTING.md](../core/docs/TESTING.md).
