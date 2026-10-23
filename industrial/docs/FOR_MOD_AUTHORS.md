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
  "energy_per_tick": 40
}
```

The format is the Core's processing recipe, described field by field in
[the Core's Machines page](../../core/docs/MACHINES.md#recipes): items and fluids in and out, the ticks it takes
(200 when left out) and the energy a tick (the machine's own when left out or 0: 40 FE for the Macerator, 30 for
the Compressor). Today's machines have one input slot and one output slot, so a recipe for them takes one kind
of item and makes one.

To replace one of the mod's own recipes, ship a file at the same path in your data pack: the mod's files are
`data/jsindustrial/recipe/macerating/iron_ore_to_dust.json`, `raw_iron_to_dust.json`,
`iron_ingot_to_dust.json`, and `data/jsindustrial/recipe/compressing/iron_ingot_to_plate.json` and
`copper_ingot_to_plate.json`.

The Electric Furnace has no kind of its own: it smelts by the game's smelting recipes (`minecraft:smelting`),
so a smelting recipe from any mod or pack works in it.

JEI and EMI show both kinds on their own pages, with the machine that works each.

## Tags

*Added 2026-06-15.*

The mod's recipes ask for common tags rather than items (`c:ores/iron`, `c:raw_materials/iron`,
`c:ingots/iron`, `c:ingots/copper`), so the ores and ingots of other mods work in them. What they make are the
Core's material items, which are in the common tags too (`c:dusts/iron`, `c:plates/iron`, `c:plates/copper`),
so other mods' recipes that ask for those tags take them.

## Capabilities

*Added 2026-06-05.*

Every machine offers the game's standard capabilities on every side:

| What | Who can reach it | Notes |
| --- | --- | --- |
| Items (`Capabilities.ItemHandler.BLOCK`) | hoppers, pipes, the computers' buses | inputs take, outputs only give |
| Energy (`Capabilities.EnergyStorage.BLOCK`) | any FE cable or generator | the Coal Generator only gives |

| Machine | Holds | Takes at most a tick | Spends a tick |
| --- | --- | --- | --- |
| Coal Generator | 16,000 FE | (gives up to 1,000) | makes 20 |
| Macerator | 16,000 FE | 1,000 FE | 40 |
| Compressor | 12,000 FE | 600 FE | 30 |
| Electric Furnace | 12,000 FE | 600 FE | 30 |

The machines take energy; they never pull it. The Coal Generator pushes into whatever touches it.

## With J's Computers

J's Industrial does not need J's Computers and does not know about it. J's Computers drives these machines the
way it drives any machine of any mod, through the capabilities above: a crafting computer's interface puts the
inputs in and a bus takes the outputs, and the Pattern Studio knows which machine works which recipe kind.

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
