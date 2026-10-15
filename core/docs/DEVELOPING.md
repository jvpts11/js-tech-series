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
