# Testing

How to test a mod built on J's Core, and the kit the Core gives for it: J's Core Test Kit.

## Two kinds of test

*Added 2026-10-05.*

- **Unit tests** (JUnit) run plain Java with no game. They are fast and exact, and they are the right test for
  logic that does not need the game: a layout class ([Screens](SCREENS.md#layouts-you-can-test)), a recipe's
  arithmetic, a parser. A class that touches Minecraft or NeoForge types cannot run in them: the game is not
  loaded, and those types fail to load or behave wrongly. Much of the Core is written so that its logic has no game
  types for exactly this reason.
- **GameTests** run inside a real server with your mod and the Core loaded, in a small arena of a real world. Use
  them for anything that touches the game: blocks, block entities, menus, capabilities, saving, networking, data
  generation's output. They catch what unit tests cannot see: a block entity type that does not list a variant, a
  capability that is not registered, a value that is not saved.

The series' own rule is simple: a test that touches the game in any way is a GameTest. And a third kind, **client
tests**, plays the game in a window, for what only a player's game shows: screens, input, what is drawn.

## The test kit

*Added 2026-10-21.*

J's Core Test Kit is a mod of its own, `jstestkit`, that loads beside the mod it tests in the GameTest server and in
the client test windows. It adds nothing to the game, is never installed and is never released: a mod depends on
it in its tests only. Its helpers live under `dev.jstech.testkit`.

For now it is published to the local Maven repository only:

```
./gradlew :testkit:publishToMavenLocal
```

```groovy
repositories {
    mavenLocal()
}
dependencies {
    implementation "dev.jstech:testkit:${jstech_version}"   // in the project that holds the tests
}
```

The series keeps its tests in a development-only mod of their own, the `tests` subproject, which depends on the kit
and is loaded beside the mods in every development run; that way no shipped jar carries test code, and a test may
span several mods. A mod can do the same, or put its tests in a source set of its own that its runs load, which is
what the Core's [Gradle plugin](GRADLE_PLUGIN.md) sets up: `src/gametest/java`, with the kit, loaded by the runs
that test.

## Arenas made in code

*Added 2026-10-21.*

A GameTest runs inside a template. The kit makes the empty ones in code: a floor of barrier blocks the size
declared, and air above it, with no structure file drawn or generated.

```java
Arenas.declare("mymod", "empty", 9, 6, 9);    // from the mod's constructor
Arenas.declare("mymod", "large", 33, 12, 33);

@GameTestHolder("mymod")
@PrefixGameTestTemplate(false)
public final class KilnGameTests {
    @GameTest(template = "empty")
    public static void kiln_smelts(final GameTestHelper helper) { ... }
}
```

An arena is in the namespace of the mod whose tests use it, as a GameTest names its template in its holder's
namespace. The game finds it where it looks for any template, and finds it again after a reload.

## Building a scene

*Added 2026-10-05.*

`ScenarioBuilder` places blocks in a GameTest's arena by positions relative to it, the way a player would place
them:

```java
@GameTest(template = "empty")
public static void kiln_smeltsWithPower(final GameTestHelper helper) {
    final ScenarioBuilder scene = ScenarioBuilder.forGameTest(helper);
    scene.placeFromItem(new BlockPos(1, 2, 1), MyContent.KILN.get());   // as a player places it, item data and all
    scene.setBlock(new BlockPos(2, 2, 1), MyContent.GENERATOR.get());
    // ...
    helper.succeedWhen(() -> helper.assertTrue(kilnFinished(helper), "the kiln has not finished"));
}
```

`layCable(level, pos, type)` lays a cable of any mod into the shared cable block, and `placeMicroblock`,
`mountPart`, `hangSmall` and `partsAt` build block spaces of parts
([Block spaces and microblocks](MULTIPART.md#testing)). A mod can extend `ScenarioBuilder` with the shapes it builds
often. It lives in the test kit, `dev.jstech.testkit.gametest`, since 2026-10-21.

## A scene for every kit

*Added 2026-10-21.*

Under `dev.jstech.testkit.kit`, a scene for each kit of the Core, each failing with a sentence that says what it
found:

| Scene | What it does |
|---|---|
| `NetworkScenes` | lays a data network of its own through the arena, which members placed beside it join; gives a request of a kind of Operation to a member that carries it out; and an Operation dispatcher ticked with the server, to queue work on and wait for its status |
| `EnergyScenes` | reads what a block holds through its port of the Core's energy or its Forge Energy, puts energy in, and a gauge that says how much it gained since it was set |
| `MachineScenes` | gives a machine of the Core's kit its inputs and its energy, and runs a recipe to its end, handing the test what came out |
| `MultiblockScenes` | builds a pattern in the arena, each letter the block the test names, and says at which place it wanted what and found what |
| `RoomScenes` | builds a closed box and finds the sealed room in it, and fills a room with gases at a pressure |
| `StructureScenes` | makes the blocks of a box a moving structure, moves it, and checks where it stands |
| `HazardScenes` | doses a body with a hazard and checks the band it stands in |
| `MenuScenes` | opens a block's menu as a player's right click does, and clicks its slots and buttons as a player's game sends them |

```java
final NetworkScenes.Network network = NetworkScenes.lay(helper, new BlockPos(2, 2, 2));
helper.setBlock(new BlockPos(1, 2, 2), MyContent.REACTOR.get());
helper.startSequence()
        .thenWaitUntil(() -> network.expectMembers(helper, 1))
        .thenExecute(network::close)
        .thenSucceed();
```

## Players in tests

*Added 2026-10-05.*

Some code asks for a player the server counts as online: a team lookup, a command, an award.

```java
final ServerPlayer player = GameTestPlayers.join(helper, "tester");
try {
    // ...
} finally {
    GameTestPlayers.leave(player);
}
```

`join` puts the player on the server's list of players without logging them in. The game's own mock player logs in,
and every mod greets a player who logs in, often with data a test's connection was never set up to carry, which
fails the test and leaves the player in the world. Always `leave` in `finally`, or the player stays for the tests
after.

A test of what a player is sent, or of a menu opened as a player opens it, needs a player who logged in: since
2026-10-21, `GameTestPlayers.connected(helper, name)` logs one in over a connection that carries every mod's payloads,
as a player's game with the same mods would. `leave` logs them out again.

## Round trips through the save

*Added 2026-10-21.*

`SaveRoundTrip.of(helper, pos)` saves a block entity as its chunk saves it, unloads it, and reads it back as the
chunk reads it when it loads, a new block entity of its type standing in its place:

```java
final SaveRoundTrip.Trip trip = SaveRoundTrip.of(helper, KILN);
trip.assertSame(helper, KILN);              // every field read back as it was saved, or each one that was not
final KilnBlockEntity kiln = trip.read(KilnBlockEntity.class);
```

What the block entity read back does as it loads (joining a network, watching a structure) it does on the next
tick, as in the game.

## Time and chance

*Added 2026-10-21.*

- `TestTime.advance(helper, pos, ticks)` runs a block entity's ticker that many times, now, so a machine that takes
  an hour of game time is shown to finish in one tick of the test; nothing else in the world moves meanwhile.
- `TestTime.sprint(helper, ticks)` has the server run its next ticks as fast as it can, as `/tick sprint` does.
- `TestChance.source(seed)` is a random source of its own, the same on every run; `TestChance.seeded(level, seed,
  work)` runs work with the level's random seeded, whole, before any other test's tick; `TestChance.seed(entity,
  seed)` seeds an entity's own.

## Assertions that explain

*Added 2026-10-21.*

`Expect` fails with what was looked at, what was expected, what was found and at which block, and the framework marks
the block in the world:

```java
Expect.equal(helper, KILN, "the kiln's output", Items.IRON_INGOT, kiln.output().getItem());
// the kiln's output: expected iron_ingot, found air at 130,-58,40 (relative: 2,2,2) (t=12)
Expect.near(helper, KILN, "the kiln's temperature", 900.0, kiln.kelvin(), 5.0);
```

`that`, `equal`, `near`, `atLeast`, `atMost`, `present` and `instance`; and `Expect.failure(helper, pos, message)`
for a failure of a test's own.

## Tests of cost

*Added 2026-10-21.*

`Costs.below(helper, pos, micros)` measures what a block entity's tick costs, on its own, and fails past the limit,
so a change that makes a machine dearer fails the build instead of being found on a server:

```java
Costs.below(helper, KILN, 25.0);   // the kiln's tick costs less than 25 microseconds
```

The ticks are run at once: first 200 to warm the code up, then 200 each timed alone, and the cost is their median,
which a pause of the collector does not move. `Costs.perTick` gives the number itself.

## Old saves

*Added 2026-10-21.*

Pieces of worlds saved by earlier versions of a mod are kept beside its tests and shown to load in each new one. A
piece is a structure file, the blocks of a few machines with everything they held; it is read back through the
game's own upgrades from the version it was saved in, and placed in the arena.

```java
for (final OldSaves.Sample sample : OldSaves.samples("mymod", "old_saves")) {
    final List<BlockEntity> placed = OldSaves.place(helper, sample, new BlockPos(1, 1, 1));
    // what came back, as each version saved it
}
```

A sample is made by a test that builds the machines and keeps them, `OldSaves.keep(helper, modid, name, from, to)`:
run with `-Djstestkit.oldsaves=<folder>`, it writes `<folder>/<version>/<name>.nbt`, to be kept among the mod's
resources under its folder of samples; run without it, keeping does nothing. The series runs it with
`-PkeepOldSaves` once for each version it releases.

## Optional mods with and without

*Added 2026-10-21.*

A mod's adapter is tested with the other mod present, as the run has it, and with its absence simulated:
`OptionalMods.without("jei", () -> ...)` has every guard of it ([`OptionalMod`](../../docs/API.md)) answer that it
is not installed while the work runs. Only what asks a guard while it runs sees the absence: an adapter started as the
game loaded stays started. `OptionalMods.violations(prefixes, roots)` is the import rule that keeps an optional mod
from becoming a required one by accident, for a unit test.

## The API's surface

*Added 2026-10-21.*

The test the series keeps its promise with ([The API](../../docs/API.md)) is offered to every mod, for its unit
tests: `ApiSurface` reads the mod's sources and follows every name its promised faces say to where it comes from.

```java
class MyModApiTest {
    private static final ApiSurface SURFACE = ApiSurface.builder()
            .mod("mymod", Path.of("src", "main", "java"))
            .photos(Path.of("src", "test", "resources", "api"))
            .build();

    @Test
    void api_keepsItsPromise() {
        assertEquals(List.of(), SURFACE.problems());
    }
}
```

The promise is what sits in an `api` package, in a package the mod's API class (any `*Api.java` in an api package,
with its `VERSION`) lists in `PROMISED_PACKAGES`, and the types `promised(...)` names. The checks, each a list of
problems: `leaks()` (a name of the mod promised by nobody), `packageProblems()`, `photoProblems()` (the API against
its photo; a failing check writes the photo it proposes to `build/api-photo`), `versionProblems()` and
`experimentalProblems()`. A unit test reaches the kit's classes alone, with none of the game: in Gradle,
`testImplementation files(project(':testkit').sourceSets.main.output)` in this repository, or the kit's jar. A kit
that enters the promise whole is marked experimental once, with `@ApiStatus.Experimental` on its `package-info`.

## What a mod uses of the Core

*Added 2026-10-22.*

The other side of the same promise: `ApiUse` reads your mod's compiled classes and checks every class, field and
method of the series they reach against the photos of the mods you build on, and anything from a package you name
as forbidden. What the photos do not promise is a problem; so is a method you override that they do not promise.

```java
class MyModApiUseTest {
    @Test
    void mod_usesOnlyWhatTheCorePromises() {
        final ApiUse use = ApiUse.builder()
                .own("com.example.mymod")
                .photoOf("core")
                .build();
        assertEquals(List.of(), use.problems(Path.of("build", "classes", "java", "main")));
    }
}
```

It reads class files rather than sources, so it sees what the compiler settled on: a method called through your
own class is followed to the class that declares it, and what the game and the language declare is yours to use.
The Core carries its photo in its jar (`META-INF/jstech/api/core.txt`), so `photoOf("core")` reads the photo of the
very Core you build against, and `photo(path)` takes one from a file. The classes of the mods you build on are read
from the test's class path, and yours from the folder you name, so the test runs after your classes are compiled
(`test.dependsOn classes`). [J's Example](../../example/README.md) runs it on every build against the
Core's photo, and forbids J's Computers, J's Industrial and the test mods.

## Data generation up to date

*Added 2026-10-21.*

`./gradlew :<mod>:checkGeneratedData` keeps the files the data run wrote before, runs it again, and fails, saying
which, when it wrote anything else; the files it wrote stay, as they are what to commit. The kit's `GeneratedData`
compares the two folders, leaving out the data generation's cache, and a unit test can call it itself.

## Long runs

*Added 2026-10-21.*

- **The report of time**: as a run of GameTests ends, `jstestkit/gametest-times.txt` beside the game lists every test
  with how long it took, the slowest first, and the log names the slowest ten.
- **The stall monitor**: a thread of the kit looks at the server every second, and when it has not finished a tick
  for a minute (`-Djstestkit.stall_seconds` to change it), it writes once, to the log and to
  `jstestkit/stall-<n>.txt`, the stack of every thread and the tests running then, while the run still hangs.

## Client tests

*Added 2026-10-21.*

A client test plays the game in a window: it builds a sequence of steps, and the kit's runner drives them on the
real client, one tick at a time, through the game's own paths: a right click is the game mode's use on a block, input
to a screen is the screen's own `mouseClicked` and `keyPressed`.

```java
public final class KilnClientTests {
    @ClientTest(timeoutTicks = 300)
    public static void kiln_opensItsScreen(final ClientTestHelper helper) {
        helper.thenServer(0, level -> level.setBlockAndUpdate(helper.abs(KILN), MyContent.KILN.get()
                        .defaultBlockState()))
                .thenTeleport(1, new BlockPos(3, 2, 7), Direction.NORTH)
                .thenRightClick(2, KILN)
                .thenAwaitScreen(KilnScreen.class, 60)
                .thenMatchesReference(0, "screen", 0, 0, 176, 166);
    }
}

ClientTests.register("mymod", "machines", List.of(KilnClientTests.class));   // from the mod's client code
```

- **Running them**: the game started with `-Djstestkit.clienttests=true` makes a fresh flat world, runs the tests,
  writes `clienttests/report.txt` in its folder (`ALL PASSED` on its last line when nothing failed) and stops, with a
  code that fails whatever started it when a test failed. `-Djstestkit.clienttests.only=kiln,press` runs the tests
  whose names hold any of those words, and `.suites=machines` the suites named.
- **Shards**: several windows side by side, `-Djstestkit.clienttests.shard=<i>` of `.shards=<n>`, share the tests by
  how many ticks each took last time (`durations.txt` beside the shards' folders), so they end together.
- **With no screen**: on a build server the windows draw on a virtual screen, as `xvfb-run` with Mesa's software
  OpenGL gives on Linux; the series' nightly run does so.
- **A helper of one's own**: a mod with steps of its own extends `ClientTestHelper` and registers its tests with
  `ClientTests.register(modid, suite, tests, helpers)`; what a mod keeps between tests that the next one must not
  inherit is put back by `ClientTests.beforeEach`.

### Pictures of the screen

*Added 2026-10-21.*

`helper.matchesReference(label, x, y, width, height)` cuts the GUI's rectangle out of the frame and compares it with
`clienttest/references/<test>-<label>.png` among the resources of the test's mod. A pixel may be a little off in each
colour, and one in two hundred may be off by more, so the small differences between one graphics card and another
pass. A picture with no reference, or one that differs more, fails the test and leaves what was drawn, and its
differences in red, in the run's `reference-pictures/<mod>` folder: look at it, and when it is right copy it into the
references, or press Take the new one on the studio's Tests page. `helper.capture` and `helper.assertSamePicture`
compare two pictures drawn in the test itself. Cut out only what the test sets: the player's inventory, the world
behind and the time of day change from one test to the next.

### Recorded tests

*Added 2026-10-21.*

A client test can also be a file: `assets/<mod>/jstech/client_tests/<name>.json`, which the studio's Record a test
writes as you play (see [While you build](DEVELOPING.md#the-tools)). Each mod's files are a suite of their own, named
after the mod, and need no code. The steps are played as they were recorded:

| Step | Played as |
|---|---|
| `use` | The block put in front of the player, and used. |
| `click` | A click on the component of that name, on a screen declared in a file. |
| `click_at` | A click that far from the middle of the screen, on any other screen. |
| `type`, `key` | What was typed, and a key pressed. |
| `wait` | That many ticks. |
| `picture` | The screen, or the menu's rectangle, compared with the reference picture of that name. |
| `close` | The screen closed. |

A test's own `ClientTests.Test(name, body, timeoutTicks)` takes any body that builds steps on its helper, as these
files do. An agent plays one in the game it is in with the MCP server's `client_test_run`, and records new ones with
`recording_start` and `recording_stop` ([While you build](DEVELOPING.md#the-mcp-server)).

## What can go wrong

- **A unit test fails with `NoClassDefFoundError` or an error from inside the game's classes.** It touched a game
  type, directly or through a class it uses. Make it a GameTest, or move the logic into a class with no game types.
- **A GameTest passes alone and fails in the full run.** Something it changed was shared (a setting, a team source,
  a static field, a sealed room) and not put back. Put it back in `finally`, or give the tests a batch of their own.
- **A GameTest fails at random.** It reads a result before the game produced it. Wait for it
  (`helper.succeedWhen`) instead of reading on a fixed tick.
- **Projectiles or entities stop at the arena's edge.** A GameTest arena is walled with barriers; start what moves
  inside it.
- **A GameTest's template is not found.** The arena is not declared, or declared in another namespace than the
  test's holder: a run that enables only some namespaces leaves out every other's.
- **A test with a player crashes the server after it ends.** The game's mock player was used: every mod greeted it
  as it logged in and it was never taken out. Use `GameTestPlayers`, and `leave` in `finally`.
- **A client test run ends at once with no test.** A name or a suite asked for matched nothing; the run says so and
  fails rather than pass with nothing run.
