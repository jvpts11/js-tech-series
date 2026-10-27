# For mod and pack authors

How J's Industrial meets other mods and data packs: its recipes, its tags, what its machines offer to pipes,
cables and computers, and its API. Most of what another mod needs goes through the game's own systems (recipes,
tags, capabilities) and through [J's Core](../../core/docs/GETTING_STARTED.md), which its machines are built on;
what J's Industrial adds on top is the small API package at the end of this page.

## Recipes

*Added 2026-10-05.*

The Macerator and the Compressor work recipe kinds of their own, `jsindustrial:macerating` and
`jsindustrial:compressing`. A recipe is a JSON file in a data pack, at `data/<namespace>/recipe/<path>.json`, so a
pack adds, changes or removes recipes with no code:

```json
{
  "type": "jsindustrial:macerating",
  "inputs": [ { "tag": "c:ores/tin", "count": 1 } ],
  "outputs": [ { "id": "mymod:tin_dust", "count": 2 } ],
  "ticks": 200,
  "energy_per_tick": 200
}
```

The format is the Core's processing recipe, described field by field in
[the Core's Machines page](../../core/docs/MACHINES.md#recipes): items and fluids in and out, the ticks it takes
(200 when left out) and the energy a tick, in joules of J's Energy (the machine's own when left out or 0: 200 J,
4 kW, for the Macerator, 150 J, 3 kW, for the Compressor). Each machine has one input slot; the Compressor one output slot, the Macerator six, for the dusts of
what an ore is mined for and the byproducts it gives. The mod's byproducts are counted, never by chance
(`"share"`, [counted byproducts](../../core/docs/MACHINES.md#counted-byproducts)).

Every recipe of the mod is written from data: the forms of its metals from their routes (which machine makes which
form, and with what yield, by hand too), its ores and minerals from the catalogue's minerals (each one's main element
and byproducts), the furnace's from the metals it uses. To replace one, ship a file at the same path in your data pack:

| Path under `data/jsindustrial/recipe/` | What it is |
| --- | --- |
| `macerating/<ore>_ore.json`, `macerating/raw_<ore>.json` | grinding an ore or its raw form: `macerating/raw_chalcopyrite.json` |
| `macerator/<metal>_ingot_to_dust.json` | grinding an ingot into dust |
| `compressor/<metal>_ingot_to_plate.json` | pressing a plate |
| `hand/<metal>_ingot_to_<form>.json` | a form by hand, with the Forge Hammer, the Draw Plate or the Mortar |
| `crafting/<metal>_<form>_to_<form>.json` | nuggets and blocks, nine for one, where the game has none |
| `smelting/...`, `blasting/...` | raw ores, ores, dusts and scraps in a furnace |

The Electric Furnace has no kind of its own: it smelts by the game's smelting recipes (`minecraft:smelting`),
so a smelting recipe from any mod or pack works in it.

JEI and EMI show both kinds on their own pages, with the machine that works each.

## Tags

*Added 2026-06-15.*

The mod's recipes ask for common tags rather than items (`c:ores/iron`, `c:raw_materials/chalcopyrite`,
`c:ingots/copper`), so the ores and ingots of other mods work in them. What they make are the Core's material items,
which are in the common tags too (`c:dusts/copper`, `c:plates/tin`, `c:wires/copper`, and `c:scraps/copper` for the
scraps, a form of the mod's own), so other mods' recipes that ask for those tags take them.

### What pipes carry

*Added 2026-10-24.*

The marks of what J's Industrial's pipes carry are fluid tags, J's Core's marks of pipes
([Marks and wear](../../core/docs/ENERGY_AND_RESOURCES.md#marks-and-wear)). A data pack puts another mod's fluids in
them, and its pipes treat those fluids as the mod's own:

| Tag | What it asks of a pipe |
| --- | --- |
| `jsindustrial:acids` | a pipe made for acids |
| `jsindustrial:molten_metal` | a pipe made for molten metal |
| `jsindustrial:molten_salt` | a pipe made for molten salt |
| `jsindustrial:ultra_pure_water` | a pipe made for ultra-pure water |
| `jsindustrial:corrosive_gases` | a gas pipe made for them; chlorine and fluorine are in it as gases |
| `jsindustrial:hydrogen` | none, but it wears plain steel through; hydrogen, deuterium and tritium are in it as gases |
| `jsindustrial:abrasive` | none, but it wears an unlined pipe through; every slurry is in it |

A pipe not made for what reaches it refuses it, and under the server's settings breaks: too hot or too cold after
five seconds, eaten by a corrosive after a minute, past its pressure at once, worn through by hydrogen after an hour or
by a slurry after ten minutes; it leaks five seconds of its flow, a gas as a cloud that poisons, smothers or explodes
as the gas does. The settings are in the server's `jsindustrial-server.toml`, section `fluids`: `pipes_fail`,
`leaks_are_hazardous` and `hydrogen_embrittles`, each on unless a server turns it off. A chemical mod's gases come into
the series' gas lines through J's Core's bridge and keep the same rules: Mekanism's hydrogen wears plain steel as the
series' does.

## The world

*Added 2026-10-24.*

J's Industrial's ores lie in **deposit types**, J's Core's veins ([World](../../core/docs/WORLD.md#veins)), each a real
kind of deposit with the minerals that form together and the ore of each of their elements beside them, at a quarter
of the weight of the minerals mined for it. They stand in for J's Core's plain veins of iron, copper, gold and tin;
the Nether's gold stays the Core's. Each is a data file a data pack can change, under
`data/jsindustrial/jstech/deposits/`:

| File | The deposit | Its minerals today |
| --- | --- | --- |
| `porphyry_copper.json` | middle to deep, in granite and andesite, huge and low-grade | chalcopyrite, pyrite, magnetite |
| `banded_iron.json` | huge and poor, in stone and deepslate | hematite, magnetite |
| `sedimentary_iron.json` | shallow to middle, in sandstone and calcite | siderite |
| `oxidised_zone.json` | near the surface, more in dry land | malachite, cuprite |
| `epithermal_gold.json` | shallow to middle, in andesite and tuff | electrum |
| `tin_greisen.json` | middle depths, in granite | cassiterite |
| `massive_sulfide.json` | middle to deep | pyrite, chalcopyrite |
| `skarn.json` | where granite meets calcite | magnetite, chalcopyrite |

Each also crosses stone and deepslate, where the game's own ores lie. The other minerals of each type join it as they
are added.

The prospecting tools speak in words, never coordinates: the direction and strength of each vein within 48 blocks
("A rich vein of Copper to the south-east"), and the reservoirs of oil, gas and brine within 128, with about how deep
each lies, in tens of blocks; the scanner, standing in a vein, its ore and the share of each material, and over a
reservoir about how much is left. A well pump drains the reservoir straight below it, a block at a time, each block of
soaked rock turning back into plain rock once it holds no more.

## Tools and armour

*Added 2026-10-24.*

J's Industrial's tools and armour are improved with **modules** that go in and come out, on J's Core's module kit
([Entities](../../core/docs/ENTITIES.md#modules-in-worn-things)), never with the enchanting table: whatever is in
`jsindustrial:not_enchantable`, of this mod or another, the table offers nothing for and the anvil takes no
enchantment onto, though it is still renamed and repaired there.

- **Sockets by tier:** two up to the T3, three at the T4, four at the T5 and T6, five from the T7, for tools and
  armour alike, of J's Core's series sockets `armour` and `tool`.
- **Pieces and seals:** a piece of armour says which it is (`head`, `chest`, `legs`, `feet`), and a closed exo-suit
  that it is `sealed`. A module of another mod that needs a seal, such as J's Space's space module, fits only the
  closed suits, never the Hydraulic Exoframe or the Industrial Exo-Suit, which are open frames.
- **Kinds of tool a server leaves out:** the `tools` section of `jsindustrial-server.toml` has a switch for each kind
  (`swords`, `pickaxes`, `axes`, `shovels`, `hoes`). Off, the kind's recipes are gone and its tools leave the creative
  tab; those already made keep working. A tool is of a kind by its tag, `jsindustrial:tool_kinds/swords` and the
  rest, so a data pack files another mod's tools under one too.

## Capabilities

*Added 2026-06-05.*

*Changed 2026-10-24: the machines run on J's Energy.*

Every machine offers these on every side its Sides tab opens:

| What | Who can reach it | Notes |
| --- | --- | --- |
| Items (`Capabilities.ItemHandler.BLOCK`) | hoppers, pipes, the computers' buses | inputs take, outputs only give |
| J's Energy (J's Core's `EnergyPorts.PORT`) | the series' cables and lines of its class | a port of low voltage; the Coal Generator only gives |
| Energy as FE (`Capabilities.EnergyStorage.BLOCK`) | any FE cable or generator | while J's Energy speaks FE, as it comes: one FE a joule |

| Machine | Power | Holds | Takes at most at once |
| --- | --- | --- | --- |
| Coal Generator | makes up to 5 kW (250 J a tick) | 2,500 J | (gives up to 2,500 J) |
| Macerator | draws 4 kW (200 J a tick) | 8,000 J | 2,000 J |
| Compressor | draws 3 kW (150 J a tick) | 6,000 J | 1,500 J |
| Electric Furnace | draws 3 kW (150 J a tick) | 6,000 J | 1,500 J |

The machines take energy; they never pull it. The Coal Generator pushes into whatever touches it, twice a second,
and burns only what is drawn from it. A machine's battery slot takes any item holding J's Energy (J's Core's
`EnergyPorts.ITEM`) or FE.

## With J's Computers

J's Industrial does not need J's Computers. J's Computers drives these machines the way it drives any machine of any
mod, through the capabilities above: a crafting computer's interface puts the inputs in and a bus takes the outputs,
and the Pattern Studio knows which machine works which recipe kind.

*Added 2026-10-27.* When J's Computers is installed, J's Industrial's integration with it (under
`dev.jstech.industrial.integration.computers`, reached only through its guard, so nothing of J's Computers is loaded
without it) brings the Industrial Controller Computer, a small computer of each of J's Computers' cases and a machine
of its own, `jsindustrial:industrial_controller`; the Fieldbus Cards, a card kind of their own,
`jsindustrial:fieldbus`, made for that machine alone; and the Industrial Control Cable, on J's Core's industrial
control line (`ControlLinks`). The controller links the machines its cable reaches, as many as its cards read: any
block that offers J's Core's `IMachineParameters` capability, so a machine of another mod built on J's Core, or one
that offers the capability itself, is linked the same way.

The controller is a member of the data network (J's Core's `INetworkMember`), of category C, and what the network
reads of it are three state tables, which J's Computers' language reads with `QUERY`:

- `jsindustrial:machines` (`QUERY machines`), a row for each machine a switched-on controller reads, where the machine
  stands (`at`): `name` (the block's and the machine's number among the controller's machines of that block, counted
  west to east, down to up, north to south: `Compressor #1`), `kind`, `state` (the machine's `MachineStatus`, as
  `fuse_blown`), `temperature` and `temperature_limit` in degrees Celsius, `heat` (the temperature as a share of the
  limit, in percent), `power` (what it draws, in watts) and `port` (its class), `progress` (in percent), `icc` (the
  controller) and `card` (its field bus and the machine's node on it, `PROFIBUS, node 5`). A value the machine does
  not offer is left empty.
- `jsindustrial:controllers` (`QUERY controllers`), a row for each controller: `name`, `cards`, `ports` (the machines
  its cards can read), `reads` and `left_out` (the machines its cables reach that no controller reads, for want of
  ports).
- `jsindustrial:alerts` (`QUERY alerts`), a row for each alert a controller keeps, newest first: `day` and `time`
  (the world's), `machine`, `alert`, `severity`, `reading`, `acknowledged`, `active` and `icc`.

It carries out three kinds of Operation, which `RUN` asks for, naming the machine as the `machines` table does or by
where it stands (`'12, 64, -3'`); `ON` names the controller, and without it the request goes to the controller that
reads the machine named. A switched-off controller, or one that reads no machine of that name, refuses it, saying why.

```
RUN jsindustrial:machine_set WITH machine = 'Compressor #1', parameter = 'on', value = 0
RUN jsindustrial:machine_set WITH machine = 'Compressor #1', redstone = 'low'
RUN jsindustrial:valve_set WITH valve = '12, 64, -3', open = true
RUN jsindustrial:station_send WITH station = '14, 64, -3', to = 3
```

`machine_set` sets any value the machine declares as settable, or what it does with redstone (`ignore`, `high`,
`low`, `pulse`). `valve_set` opens or closes a valve opened by hand (J's Core's `LineValveBlockEntity`, of any mod).
`station_send` has a capsule station of a pneumatic post (J's Core's `CapsuleStationBlockEntity`, of any mod) send what
waits in it to an address; while the station cannot send yet, without its compressed air, the request waits, and it
is done once the capsule leaves. J's Industrial ships no valve or station blocks of its own yet: the requests reach
those of any mod built on J's Core's, and its own once they come.

A switched-on controller looks at its machines once a second and raises an alert the moment one falls into a fault,
marking it over once the machine is out of it. A machine of any mod raises them through the same capability, from
what it already offers: its `MachineStatus` (`overheated` raises `overheating` and `fuse_blown` raises `blown_fuse`,
both critical; `undervoltage`, a warning; `damaged`, a notice), and the gas at its place, from J's Core's gas clouds
and the air of its sealed room (`gas_leak`, a warning, past a poison's IDLH or a fuel's lower explosive limit; the
air's own oxygen, nitrogen and argon never count). Each alert goes into the network's event log as it is raised, as
an error, a warning or information by its grade. A controller keeps its latest 64; acknowledging one only marks it.

The Machine Monitor, a program by Wonderwork Systems (`jsindustrial:machine_monitor`, from the Legacy era on), shows
the alerts of every controller on its computer's network and acknowledges them. It is registered as any add-on
program is, through J's Computers' `ComputersRegisterEvent` and `DesktopApps`, and asks the server for what it shows
with its own payloads (`jsindustrial:machine_monitor_request`, `..._acknowledge` and `..._answer`), each checked
against the computer it is open on.

## The API

*Added 2026-10-23.*

J's Industrial's API is the package `dev.jstech.industrial.api`. Like J's Core's, it is versioned with one number,
`JsIndustrialApi.VERSION` (1 today), raised whenever something is added, and everything in it is experimental until
it has been through a release. Everything else in the mod is its own business and changes without warning. Nothing
in it registers itself by annotation: it is declared through builders and constructors while the game loads.

An add-on that needs something added later refuses to load below the number that added it:

```java
if (JsIndustrialApi.VERSION < 1) {
    throw new IllegalStateException("MyAddon needs J's Industrial with API version 1 or newer");
}
```

**Consequences.** Every consequence of J's Industrial (overvoltage burning machines, pipes corroding, boilers
bursting) is a setting of its own, on unless a server turns it off, and off it gives way to what harms nothing; no
setting turns a part of the mod off as a "module". `Consequences.of` makes such a switch in your own settings file,
the J's Core way ([Settings](../../core/docs/SETTINGS.md)):

```java
public static final ConfigKey<Boolean> BOILERS_EXPLODE = Consequences.of("fluids.boilers_explode",
        "Boilers explode", "A boiler held past its pressure bursts, as a real one does.",
        "a blocked boiler just stops.");
```

**Advancements.** The mod's advancements are in four tabs, by the phases of its industry: Foundation (opened by the
first machine), Industrialisation (the third tier), Discovery (a team's first discovery) and Transcendence (the
sixth tier). An add-on hangs its own under one by naming a tab's root as its parent, in a data pack's advancement
file (`"parent": "jsindustrial:foundation/root"`) or from code:

```java
ResourceLocation parent = IndustrialAdvancementTab.FOUNDATION.root();   // jsindustrial:foundation/root
```

**Sounds.** The mod's sounds are kept in groups (`IndustrialSoundGroup`: machines, power, air and fluids, safety, the
Clean Room, science, transcendence), each mixed in one of J's Core's channels, so a player sets their volume where
they set every other machine's or alarm's. An add-on's sound of the same kind goes in the group's channel:

```java
AudioChannel channel = IndustrialSoundGroup.SAFETY.channel();   // the Core's Alerts channel
```

A group of machines grows louder as the machine draws more power (`followsPower()`), and the safety group carries
far, to warn in time (`carriesFar()`).
