# For mod and pack authors

How J's Industrial meets other mods and data packs: its recipes, its tags, and what its machines offer to pipes,
cables and computers. It has no API package of its own: everything another mod needs goes through the game's
own systems (recipes, tags, capabilities) and through [J's Core](../../core/docs/GETTING_STARTED.md), which its
machines are built on.

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
