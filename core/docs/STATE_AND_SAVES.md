# State and saves

The parts of J's Core that keep things: values saved with the world for a server, a dimension, a player or a team;
versions of what is saved, so an older save still loads; values read from data packs; and a few tools that work
over time: moving several things at once or not at all, sharing a tick fairly, the world's calendar, timings, and
the Core's own event bus.

## Values kept with the world

*Added 2026-10-01.*

A value your mod keeps (a counter, a list of claims, a player's unlocked recipes) is declared once with its type,
its default and who it belongs to:

```java
public static final PlayerState<Integer> KILNS_BUILT = CoreState.builder(rl("kilns_built"), Codec.INT, 0)
        .synced(ByteBufCodecs.VAR_INT)     // also send it to the player it belongs to
        .player();

// in your mod's constructor, on both sides:
CoreStates.register(KILNS_BUILT);

// on the server:
KILNS_BUILT.update(server, player.getUUID(), n -> n + 1);
int built = KILNS_BUILT.get(server, player.getUUID());
// on the player's side, when synced:
int mine = KILNS_BUILT.client();
```

| Ends with | Kept per | Read and changed with |
| --- | --- | --- |
| `.server()` | the whole server | `get(server)`, `set`, `update` |
| `.dimension()` | each dimension | `get(level)`, `set`, `update` |
| `.player()` | each player, online or not | `get(server, uuid)`, `set`, `update`, `all(server)` |
| `.team()` | each team ([Ownership](OWNERSHIP.md)) | `get(server, team)`, `ofPlayer(server, uuid)`, `set`, `update` |

- Each value is saved in its own file with the world, named after its id unless `.fileName(...)` says otherwise.
- Values are read and changed on the server's thread. Hand it a new value rather than changing the old one in
  place: the state knows a value changed by being given the next one.
- A synced value is sent at the end of the tick it changed in, however many times it changed, and only to the
  players it belongs to (the server's to everyone, a dimension's to those in it, a player's to that player, a
  team's to its members). Players are sent their values when they join, change dimension, respawn and change team.
- Using a value before it is registered throws, and so does registering two under one id.

## Versions of what is saved

*Added 2026-10-01.*

What your mod saves will change shape as your mod changes. A **layout** numbers each shape and says how to read an
older one:

```java
.version(2)
.upgrade(1, ISaveUpgrade.compound(old -> {
    old.putInt("heat", old.getInt("temperature"));   // version 1 called it "temperature"
    old.remove("temperature");
    return old;
}))
```

`CoreState.builder(...)`, a block entity's fields (`fields().layout(...)`) and `SaveLayout.builder(name)` for your
own files all take the same steps. What was saved before any version was given counts as version 0. When the
game loads something older, it runs every step from that version up, in order. When it finds something
**newer** (the world was opened with a newer version of your mod, then an older one), it reads what it can and
never writes over it: a copy of the newer file is kept beside the one it writes, so going back to the newer mod
loses nothing.

## Values from data packs

*Added 2026-10-01.*

A value a pack maker should be able to change (a machine's table of fuels, a list of rewards) lives in a data
file and is read again whenever data packs reload:

```java
public static final DataRegistry<Fuel> FUELS = DataRegistry.builder(rl("fuels"), "mymod_fuels", Fuel.CODEC)
        .synced()                                   // the players' side gets them too
        .register();

Optional<Fuel> coal = FUELS.get(rl("coal"));        // data/mymod/mymod_fuels/coal.json
Map<ResourceLocation, Fuel> all = FUELS.entries(level);
```

Every file in `data/<namespace>/<folder>/` is one entry, named by its namespace and path. A file that does not
read is skipped with a line in the log. `onReload(...)` runs code each time they are read.

## Several moves, all or none

*Added 2026-10-01.*

Taking from one place and putting into another can half succeed: the items left the chest, and the machine was
full. A `Batch` tries every step first and only then does them, undoing what it did if one comes short:

```java
Outcome outcome = new Batch()
        .add(HandlerSteps.extract(chest, new ItemStack(Items.IRON_INGOT), 3))
        .add(HandlerSteps.insert(machine, new ItemStack(Items.IRON_INGOT, 3)))
        .commit();
```

`fits()` only tries. The steps cover items (`IItemHandler`), fluids and energy. `commit()` answers `DONE`,
`REFUSED` (a step would have come short, so nothing moved), `UNDONE` (a step came short while moving, and every
step was undone) or `STUCK` (a step came short while moving, and one already done could not be wholly given back).
That last one is rare and is yours to report. Two steps into the same store are only found to clash when they
move, since trying each alone cannot see the other.

## Sharing a tick

*Added 2026-10-01.*

A block that runs many things each tick (a computer running programs) must share its time fairly and stop when
the tick's time is up. A `TickScheduler`, one per block, does it: each tick hand it the tasks and the credit, and
it gives each a fair share, in turns, starting next tick with whoever was not served this one.

```java
TickScheduler.Outcome spent = this.scheduler.run(tasks, credits, System::nanoTime, deadline);
```

A task (`TickScheduler.ITask`) does up to the budget it is handed and answers how much it did. The outcome says
what was spent and whether the deadline cut the tick short; credit one task leaves unused goes to the others.

## The calendar

*Added 2026-10-01.*

`GameCalendar` reads the world's time as a calendar: `GameCalendar.hourOf(level.getDayTime())`, `dayOfWeek`,
`week`, `hoursBetween`; and with the server's days per season, `WorldCalendar.now(level)` gives the day, hour,
minute, weekday, week, season and year at once. A day is 24,000 ticks, and starts at 06:00, as the game's clock
does.

## Timings

*Added 2026-10-01.*

```java
final long start = System.nanoTime();
// ... the work ...
Diagnostics.record(rl("kiln_tick"), start);
```

The last 100 timings of each section are kept, and the slowest on average show in the right column of the F3
screen. `DiagnosticsClient.panel(title, lines)` adds a panel of your own lines there.

## The Core's event bus

*Added 2026-06-04.*

Besides NeoForge's event buses, the Core has a small one of its own for what happens in its systems:
`JsCore.events()`. It is plain Java, runs listeners on the caller's thread in the order they subscribed, and
carries the life of every Operation (created, started, completed, failed, discarded) and the network's own events.

```java
JsCore.events().subscribe(IOperationLifecycleEvent.class, event -> log(event));
```

Subscribing to a sealed family of events hears every member of it. A cancellable event that is cancelled skips the
listeners after.

## What can go wrong

- **"state ... is not registered".** `CoreStates.register` was not called for it, or not on both sides.
- **A player's side shows the default.** The value is not `.synced(...)`, or it belongs to someone else (a
  player's value is only sent to that player).
- **An older world loses a value.** The shape changed without a version and an upgrade step.
- **A data file is ignored.** It does not read with the codec (the log names the file), or it is in the wrong
  folder.
