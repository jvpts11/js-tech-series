# While you build

What the Core does for you while you write a mod on it: errors that say how to fix them, a data run that fails
when something would show broken, a log you can turn up one part at a time, and data and assets reloaded as you
change them.

## Listeners are handed over, not found

*Added 2026-10-15.*

A mod hears the game's events by handing a listener to an event bus. The game can also find listeners by itself,
in classes marked `@EventBusSubscriber`, but the series does not use that: each of its classes that listens has a
`listen` method, and the mod's constructor calls every one of them, so what runs is exactly what the constructor
says.

```java
public final class MyNetwork {

    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, MyNetwork::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, MyNetwork::afterServerTick);
    }
    ...
}

// In the mod's constructor, which is handed modBus:
MyNetwork.listen(modBus);
```

Events of loading and registering (payloads, capabilities, data generation, models) go on the bus the
constructor is handed; events of the running game (ticks, players, worlds) go on `NeoForge.EVENT_BUS`. A class
that only draws on a player's screen is listened for from the mod's client constructor
(`@Mod(value = MODID, dist = Dist.CLIENT)`).

## Errors that teach

*Added 2026-10-15.*

When something you declared cannot stand, the game stops loading with one paragraph that says which mod, which
declaration, what is wrong and how to fix it:

```
mymod declares the block mymod:press, which has no name. To fix it, give it its English name with named(...)
before register().
```

The Core's content builders, settings, registries and data generation all report their mistakes this way, as a
`DeclarationError`. You can throw one from your own declarations too:

```java
throw DeclarationError.of(MODID, "the recipe " + id, "makes nothing",
        "give it a result with result(...)");
```

## A data run that fails when something is missing

*Added 2026-10-15.*

End your data generation with `data.check()` (see [Getting started](GETTING_STARTED.md)). The run then looks
for every block and item your mod registered, in what it wrote and in your resources, and fails, saying what to
add, when one has no English name, no block state, no model, a model whose texture is nowhere, or no loot table
while it drops something. What would be a magenta cube or a raw key in the game is found before you start it.

## The log, one part at a time

*Added 2026-10-15.*

Each part of a mod writes to the log under a name of its own, `mod/system`, and how much it writes can be
changed while the game runs. Declare your systems once, as constants, and write through them:

```java
public static final LogSystem MACHINES = LogSystems.declare(MODID, "machines");
...
private static final Logger LOGGER = MyLogs.MACHINES.logger();
LOGGER.debug("the press at {} took {}", pos, stack);
```

In the game, `/jstech log` lists every system and its level, `/jstech log mymod:machines` says one system's, and
`/jstech log mymod:machines debug` turns it up until the game stops, so you can follow a fault in your machines
without the rest of the game filling the log. The levels are `error`, `warn`, `info`, `debug` and `trace`. From
code, `LogSystems.setLevel(MyLogs.MACHINES, LogLevel.DEBUG)` does the same.

## Data and assets reloaded as they change

*Added 2026-10-15.*

In a development run (`runClient`, `runServer`), the Core watches the `assets` and `data` folders your mod is
loaded from. When files there change and then stay as they are for a moment, the player's game reloads its
resources and the server its data packs, as F3+T and `/reload` would. Edit a texture or a recipe, build the
resources (in IntelliJ, Build), and look again: nothing to press. A packed game watches nothing.

*Added 2026-10-16.* A change to nothing but the files of the Core's models (anything in a `jsmodels` folder, or a model
of a format only the Core reads) reads the models alone again, at once, instead of every resource; an open preview of
a model shows the change ([Models](MODELS.md#while-you-build)).

*Added 2026-10-21.* A change to nothing but the manuals' files (anything in a namespace's `guide` folder) reads the
manuals alone again, at once, and an open manual lays itself out anew where its reader is
([Manuals](MANUALS.md#writing-while-the-game-runs)).

## Developer mode

*Added 2026-10-21.*

The tools for whoever makes a mod or a modpack (the studio, the inspector and editor of screens, the new screen) open
only while **developer mode** is on. It is two keys:

| Key | File | What it says |
|---|---|---|
| `developer.mode` | `jstech-developer.toml`, the player's own | Whether the tools are on in this game. |
| `developer.server` | `jstech-developer-server.toml`, the world's | Whether this world lets them be used. |

Both are on in a development run and off in a packed game, and both are in the game's settings screen. A tool opens
only with both on, so a player cannot turn the tools on against a server that keeps them off. The tools that read or
change the world, as running a thing's GameTests, are only for the player whose game runs the world, or for the
players holding the permission `jscore.developer.tools` (operators, unless the server's permission mod says
otherwise). From code:

```java
DeveloperMode.on();                       // on the player's game: both keys on
DeveloperMode.mayUseWorldTools(player);   // on the server: the world's key on, and the player allowed
```

## The studio

*Added 2026-10-21.*

`/jstech studio`, or the key "Open the studio" once you give it one in the controls, opens the studio over the world.
It is built around the things of the game, never around kinds of files:

- **On the left, every thing of every mod on the Core** and of every content pack, by mod and by kind: machines (a
  block whose block entity is a machine's), blocks, items, fluids, entities, cables and parts, and research. Each mod
  and kind says how many things it holds, and each thing how many problems it has, with the worst one's mark. The
  search finds a thing by its name or its id. **Checks: N** counts every problem of every thing, and a click on it
  goes to the next thing that has one.
- **In the middle, the thing on a bench**: a block on a floor of stone in a small world of its own, drawn again each
  frame so it moves as it does in the world. Drag to walk round it and turn the wheel to come closer. A machine has
  three buttons, Working, Idle and Jammed, which tell its block entity it is working, waiting for input or with its
  output full, so you see each look its model has. A creature stands on the bench; anything else is held up large.
- **On the right, what the thing is made of**, each part the ordinary file it always was, with its state: its model
  (the states file, or the GeckoLib model), its screen, its effects, its sounds, its names in each language its mod
  has, the recipes that make it, its manual page, its tags, its drops, its settings and the GameTests that name it. A
  click on a part opens its file in what your computer opens such files with.
- **Under the bench, the checks across its parts**: what no single file can see on its own. The Core checks that a
  thing is named in every language its mod names things in, that its states file and models are there and the game
  baked a model from them, that no model names a texture that is not there, that no animation moves a bone no model
  of its mod has, that its screen file loads, that its item is in a creative tab, and that a block that drops by a
  loot table has one (in a world of your own, whose data the studio can read). Open opens the file that mends it.

**Reload all** reloads the resources, and the data when the world is yours, and reads every thing again. **Run its
tests** asks the server to run, near you, the GameTests whose name names the thing, and the chat says how many
passed. A test's name is its class and its method, and only their letters and digits are compared: the thing
`compressor` is named by every test of `CompressorTests` and by `MachineTests.compressor_jamsWhenFull`.

A part of your own joins the others with a reader, from your client's setup. This one shows a thing's notes, a file
of your mod's beside its models:

```java
StudioReaders.add((thing, context, sheet) -> {
    final ResourceLocation notes = ResourceLocation.fromNamespaceAndPath(thing.id().getNamespace(),
            "notes/" + thing.id().getPath() + ".md");
    if (context.exists(notes)) {
        sheet.part(new StudioSheet.Part(new ItemStack(Items.WRITABLE_BOOK), "Notes", notes.getPath(),
                StudioSheet.Mark.FINE, Optional.of(StudioFiles.asset(notes))));
    }
});
```

A reader that throws is written down as a problem of the thing's, and the other readers still read it.

### The tools

*Added 2026-10-21.*

Under the bar, a strip of tabs reaches the studio's tools. Each opens on the block you looked at when you opened the
studio, or on the thing chosen on **Things**, and the studio opens again on the tab it was left on. The tools that read
the world ask the server for it, so they answer only a player who may use the world's tools (see above).

| Tab | What it shows and does |
|---|---|
| **Block** | Every field the block entity saves, read again each second while Watching is on; what each face offers (energy, slots and what they hold, tanks); and, for a block entity of the Core, what crossed its faces each second, energy, items and fluid, in and out. Only what really moved is counted, never a move that was only tried. |
| **Cost** | Measure times every machine for some seconds and lists them dearest first, with where each one is and who owns it. Heat map lights each one in the world, the dearest red and the cheapest yellow. |
| **Payloads** | What the server sent you, or everyone, tick by tick over the last hundred ticks: a bar a tick and, for the tick chosen, each kind of packet with its mod, how many, how many bytes and the biggest one. |
| **Recipes** | The recipes that make the thing chosen, each with the pack it came from and what a pack took away, replaced or changed; the recipe as the recipe viewers draw it; the other recipes that take the same inputs; and a recipe of your own, written, tried and saved to your mod. |
| **Tags** | The tags the thing is in and, for each one, which pack put each entry there and whether a pack replaced what came before; its id, its stack size and its components. |
| **Loot** | A loot table rolled a hundred, a thousand or ten thousand times with the tool, the Fortune or the Silk Touch chosen: how often each thing dropped, how many a roll, the fewest and the most. |
| **Translations** | Every mod with how many keys it has; what a language lacks of the English; and, in every screen declared in a file, the texts too long for their place in each language. |
| **Settings** | Every setting of every mod's files with its value, its default and its range, a value the pack changed in yellow. Export the pack's preset writes them all as the preset `pack`. |
| **Structures** | Two corners taken from the blocks you look at; the blocks between them counted as a structure file holds them, saved to your mod as `data/<mod>/structure/<name>.nbt`, and shown on the page and in the world as the Structure Projector shows a multiblock. |
| **Tests** | Record a test closes the studio and keeps what you do: the block you use, each click on a screen (by its component's name, on a screen declared in a file), what you type and the keys, the waits and the pictures you take. Keep it writes a client test of the mod, beside its resources (see [Testing](TESTING.md#recorded-tests)). Every mod's reference pictures, each beside what the last run drew and the difference; Take the new one makes the drawn picture the reference. |
| **Save** | The block looked at saved and read back, as a world closed and opened again does, field by field; a field that comes back other than it went is marked. |
| **Logs** | Every log system with the level it writes at, changed with a click; its last lines; and the errors that teach that the game has made since it started. |

A mod's own tool for the studio's pages, or for a screen of its own, asks the server the same way. The server's side
is declared once, and answers at once or later:

```java
StudioServices.declare("mymod:light", (player, args, reply) -> {
    final CompoundTag answer = new CompoundTag();
    answer.putInt("light", player.serverLevel().getMaxLocalRawBrightness(BlockPos.of(args.getLong("pos"))));
    reply.accept(answer);
});

StudioAnswers.ask("mymod:light", args, answer -> shown = answer);   // on the player's game
```

The answer to a player who may not use the world's tools holds `StudioServices.REFUSED`, with why.

## Tools for screens

*Added 2026-10-21.*

On a screen declared in a file, F12 opens the inspector and Ctrl+E from it the editor, which writes every change to
your mod's source; `/jstech screen new` makes a new screen for the block you look at. All three are in
[Screens](SCREENS.md#tools-for-building-a-screen), and open while developer mode is on.

## What can go wrong

- **A listener never runs.** Its class's `listen` is not called from the mod's constructor, or a client class is
  called from the common one (it belongs in the client constructor), or the event went on the wrong bus: loading
  events on the constructor's bus, the running game's on `NeoForge.EVENT_BUS`.
- **The data run fails after the files were written.** That is `check()` at work: read the list it prints, add
  what it names, and run again.
- **`/jstech log` does not list a system.** Its class was never loaded: touch the class that declares your systems
  from your mod's constructor, or write through it once.
- **Nothing reloads when you save.** The game reads your files from the build's folders, not from `src`: build the
  resources so the changed file is copied there.
- **`/jstech studio` says developer mode is off.** Turn on "Developer mode" in the settings; on a server, its
  "Developer mode on this world" has to be on too.
- **Open says a file is not among the mod's sources.** Your mod is loaded from a jar, not from its project's
  `build/resources/main`, so the run cannot find its `src/main/resources` (nor `src/generated/resources` beside it).
- **Run its tests is grey.** No GameTest names the thing, or the GameTests are not on in this run: a run turns them on
  with the property `neoforge.enabledGameTestNamespaces`.
