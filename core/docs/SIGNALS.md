# Redstone and signals

Signals carried by the game's redstone: every sensor speaks redstone through one capability, and signals travel
without wire by frequency, from transmitters to receivers, as with Create's redstone links and the wireless redstone
mods. The series has no logic gates and no cables of many channels: logic belongs to J's Computers.

The design behind this page is [Redstone and signals](design/redstone-and-signals.md).

## Every sensor speaks redstone

*Added 2026-10-15.*

A sensor, or a machine, says what it reads as a list of `Reading`s: what it reads (a `TextKey`), its value now, the
span it is read over (the value that gives a signal of 0, the one that gives the full 15) and its unit. Light, heat,
speed, a room's pressure, radiation, altitude, a machine's declared parameter: each is a reading.

```java
new Reading(SKY_LIGHT, light, 0, 15, null).strength();         // 0 to 15, the value outside its span held to it
```

A block offers what it reads with the capability `Signals.OUTPUT` (`ISignalOutput`): its `readings()`, and which of
them its signal speaks, `chosen()`. Anything reads every sensor alike, `Signals.strengthAt(level, pos, face)`: a
comparator's worth of redstone, and J's Computers through its peripheral links. A node of the data network that is
inspected, `IMonitorable`, reports the same `readings()`.

The kit gives a sensor whole. A mod writes only its block entity, with what it reads:

```java
public final class LightSensorBlockEntity extends SensorBlockEntity {

    @Override
    public List<Reading> readings() {
        return List.of(new Reading(SKY_LIGHT, level.getBrightness(LightLayer.SKY, worldPosition.above()), 0, 15,
                null));
    }
}

// Its block, a SensorBlock, gives the redstone on every face and to a comparator, and reads every ten ticks.
// In RegisterCapabilitiesEvent:
Signals.offer(event, LIGHT_SENSOR_ENTITY.get());
```

`choose(index)` sets the reading its signal speaks. What a block gives is kept in a `SignalLevel`
(`SignalLevel.in(fields(), "signal")`), saved with it: its neighbours are told only when it changes, so a sensor read
every ten ticks in a steady world costs its neighbours nothing.

## Signals by frequency

*Added 2026-10-15.*

A transmitter sends one value, from 0 to 15, on a numbered frequency; every receiver on that frequency gives it out on
every face. A frequency has no channels: two signals take two frequencies. What a radio is tuned to is a `Tuning`:

| Band | Whose frequencies | Made with |
| --- | --- | --- |
| `PRIVATE` | the owner's own: nobody else's radio hears them | `Tuning.on(Band.PRIVATE, 7)` |
| `TEAM` | the owner's team's ([Ownership](OWNERSHIP.md#teams)): every player of the team hears them | `Tuning.on(Band.TEAM, 7)` |
| `PAIR` | a link of its own between one transmitter and the receivers given its pair, with no number | `Tuning.newPair()`, `Tuning.paired(id)` |

Numbers run from 1 to 9,999. Frequency 7 of a player and frequency 7 of their team are two frequencies.

- **One transmitter, any number of receivers.** A second transmitter on a frequency of the same owner is a **clash**:
  its receivers give out nothing until one of the two is taken off, and each transmitter knows it clashes.
- **Real reach.** A transmitter is heard as far as its power, in blocks of open air; each solid block in the way takes
  `RadioReach.OBSTACLE_COST` (4) blocks of it. Within reach the value arrives whole, whatever the distance; beyond it
  nothing arrives, and the receiver gives out 0. How well it arrives is told in bars, from one at the edge of its
  reach to four near it (`RadioReach.bars`): margin, never value.
- **Relays.** A relay sends again, with its own power, every frequency it hears of its owner's team. A relay may have a
  **partner**, a relay elsewhere, in this dimension or another, which sends again all it hears: between dimensions a
  signal travels only so (J's Space's satellites are relays).
- **On the air while loaded.** A radio is on the air while its chunk is loaded; the walls of unloaded chunks are not
  counted.

The kit gives the radios as blocks: a `RadioBlock` of each role, all sharing one `RadioBlockEntity` type of your mod.

```java
public static final BlockEntry<RadioBlock> TRANSMITTER = CONTENT.block("transmitter",
        properties -> new RadioBlock(properties, IRadio.Role.TRANSMITTER, 64, () -> RADIO.get())).register();
public static final BlockEntry<RadioBlock> RECEIVER = CONTENT.block("receiver",
        properties -> new RadioBlock(properties, IRadio.Role.RECEIVER, 0, () -> RADIO.get())).register();
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioBlockEntity>> RADIO =
        CONTENT.blockEntity("radio", (pos, state) -> new RadioBlockEntity(RADIO.get(), pos, state),
                TRANSMITTER, RECEIVER);
```

Whoever places a radio owns it. A transmitter sends the strongest redstone fed into it; a block that works out its
value another way calls `send(value)`. `tune(tuning)` tunes it, `own(ownership)` hands it over, and a relay's
`link(partner)` gives it a partner. A block entity of your own may be a radio too: implement `IRadio` and join
`SignalRadio` as it loads (`join`) and leave as it goes (`leave`), calling `retune` when it is tuned again and `sent`
when its value changes.

**No storms of updates.** Nothing is worked out on every tick. Who hears whom is worked out again only when a radio on
a frequency comes, goes or is tuned again, and every `SignalRadio.RECHECK_EVERY` (100) ticks for the walls built and
broken meanwhile; a new value is handed only to the receivers already known to hear it, at the end of the tick; and a
receiver's neighbours are told only when what it gives out changes.

## The signal probe

*Added 2026-10-15.*

The series' **Signal Probe**, of the network probe's family, reads it all: used on a block it reads the redstone on it,
now and its peak since; wherever it is held it lists the frequencies heard there (`SignalRadio.heardAt`), each with
whose it is, where it comes from, its bars and its value, clashes marked, and the frequencies of the player's team
that are not heard there, so a player walks to find where a receiver or a relay will hear well. OK on its radio's page
outlines where the chosen one comes from.

## What can go wrong

- **A receiver gives nothing.** Its transmitter is another owner's on a private frequency, two transmitters clash on
  it, or it is out of reach: walk there with the signal probe.
- **A frequency is heard on one side of a wall and not the other.** Each solid block takes four blocks of reach; glass,
  leaves and other blocks that let light through take none.
- **A team's frequency is not shared.** The players are on teams of different sources: with FTB Teams installed, the
  `/team` teams are not asked ([Ownership](OWNERSHIP.md#what-can-go-wrong)).
