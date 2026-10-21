# The toolkit

The small kits nearly every technology mod needs, done once in the Core: capabilities of your own, calls from a
screen to the server, work away from the tick, codecs for records, and other mods you work with when they are
installed. All of them, the capabilities with them, are promised to addons ([docs/API.md](../../docs/API.md)),
experimental for now. The two of the series' own the capability kit still holds, what a node of the data network and
what the network may inspect implement, move to the series' package later, with a note in the changelog.

## Capabilities of your own

*Added 2026-10-15.*

A capability is how a block, an item or an entity hands something over to whoever asks: the game's own ones hand
over inventories, tanks and energy. Declare one of your own in one line, for blocks, items and entities at once:

```java
public static final CapabilityKind<IHeatHandler> HEAT = CapabilityKinds.declare(rl("heat"), IHeatHandler.class);
```

Offer it while the capabilities are registered (from your constructor:
`modBus.addListener(RegisterCapabilitiesEvent.class, MyCaps::register)`), by face for a block or an entity:

```java
HEAT.onBlockEntity(event, MY_FURNACE_BE.get(), (furnace, face) -> furnace.heat(face));
HEAT.onItem(event, (stack, context) -> new ItemHeat(stack), MY_HOT_ROCK.get());
```

And ask for it where you need it: `HEAT.at(level, pos, face)`, `HEAT.of(stack)`, `HEAT.of(entity, face)`.

### Asking every tick: the cache

Something that asks the same neighbour every tick keeps a `CapabilityCache` instead. Asking it costs a field read;
the game tells it when the block there is placed, broken or changes what it offers, and only then is the answer
worked out again:

```java
output = CapabilityCache.of(Capabilities.ItemHandler.BLOCK, level, pos.east(), Direction.WEST, this);
...
IItemHandler target = output.get();
```

It works for any capability, the game's own included, and lets go once its owner is removed.

### Parts on a block

A part mounted on a face of a cable block (a bus, a sensor) offers what it offers by overriding `capability` in
its `IFacePart`; the cable block answers with it on that face, for the game's items, fluids and energy and for
every capability declared with `CapabilityKinds`. The part registers nothing. A block of your own that holds parts
does the same with one line, `PartCapabilities.register(event, MY_HOST_BE.get(), MyHost::parts)`.

## Calls from a screen to the server

*Added 2026-10-15.*

A button on a screen that changes something on the server needs a message to the server. Declare the call once, on
both sides, and there is nothing else to write:

```java
public static final MenuCall<PressMenu, Integer> SET_SPEED = MenuCalls.declare(rl("set_speed"), PressMenu.class,
        ByteBufCodecs.VAR_INT, (menu, player, speed) -> menu.press().setSpeed(speed));
```

The screen calls `SET_SPEED.send(3)`. The server handles it only while the player still has a `PressMenu` open and
in reach: the client is never trusted to say which block it means, the menu the server opened is the proof. Every
call travels in one payload the Core registers.

## Work away from the tick

*Added 2026-10-15.*

Work that takes long (searching a large network, reading a file, planning) must not run in the server's tick, or
the whole server waits for it. Hand it to `Tasks.onServer`:

```java
Tasks.onServer(() -> planner.plan(request), plan -> machine.start(plan));
```

The work runs on a virtual thread of its own, where it must not touch the world; its result comes back on the
server's thread at the end of a tick, where it may, within 2 ms of each tick, so a burst of finished work never
stretches one. `onServer` hands back a `Task`, which `cancel()` stops; whatever has not come back when the server
stops is cancelled. For a main thread of your own, make a `TaskRunner` and `drain` it there.

## Codecs for records

*Added 2026-10-15.*

A codec says how a value is written into a save, a data file or a message, and read back. A record's codec is
written one line a component, in the record's order, with no limit on how many:

```java
public static final Codec<Setting> CODEC = RecordCodecs.of(Setting.class)
        .field("name", Codec.STRING)
        .field("level", Codec.INT)
        .optional("shown", Codec.BOOL, true)
        .build();
```

The names are checked against the record's as the game loads. The codec it travels the network with is taken from
it, so the two never disagree: `NetworkCodecs.fromSave(CODEC)`.

## Other mods, when they are installed

*Added 2026-10-15.*

Working with another mod when it is installed must never make it required. Put the work with it in an adapter class
under an `integration` package, and start it through its guard, which loads nothing of the other mod unless it is
there:

```java
public static final OptionalMod JEI = OptionalMod.of("jei");
...
JEI.ifLoaded(() -> JeiAdapter::start);   // in your constructor
```

An adapter that fails to start (the other mod changed under it) is written to the log and left out; the game goes
on. Keep the rule with a unit test of your own, which reads your sources and fails on an import in the wrong place:

```java
assertEquals(List.of(), OptionalDependencyCheck.of(List.of("mezz.jei."))
        .violations(List.of(Path.of("src", "main", "java"))));
```

## What can go wrong

- **A part's capability is not found.** The capability is declared after the capabilities were registered: declare
  it as a constant in a class your constructor loads.
- **A call does nothing.** The player's open menu is not of the call's kind, or is out of reach; or the call was
  declared on one side only.
- **A task's result never comes back.** It was cancelled, or the work threw: the failure goes to the log, or to the
  `onFailure` you gave.
- **The game will not load: the codec's fields are not the record's.** Write one field a component, with its name,
  in the record's order.
- **The game will not start without the other mod.** A class of it is named outside the guard: the check's test
  says where.
