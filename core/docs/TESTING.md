# Testing

How to test a mod built on J's Core, and the kit the Core gives for it.

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

The series' own rule is simple: a test that touches the game in any way is a GameTest.

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

`layCable(level, pos, type)` lays a cable of any mod into the shared cable block. A mod can extend
`ScenarioBuilder` with the shapes it builds often.

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

`join` puts the player on the server's list of players without logging them in. A real login would have every mod
greet a connection the test cannot carry, and fail. Always `leave` in `finally`, or the player stays for the tests
after.

## What can go wrong

- **A unit test fails with `NoClassDefFoundError` or an error from inside the game's classes.** It touched a game
  type, directly or through a class it uses. Make it a GameTest, or move the logic into a class with no game types.
- **A GameTest passes alone and fails in the full run.** Something it changed was shared (a setting, a team source,
  a static field) and not put back. Put it back in `finally`.
- **A GameTest fails at random.** It reads a result before the game produced it. Wait for it
  (`helper.succeedWhen`) instead of reading on a fixed tick.
- **Projectiles or entities stop at the arena's edge.** A GameTest arena is walled with barriers; start what moves
  inside it.
