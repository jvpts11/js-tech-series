# Heat

Temperature held in blocks, in kelvin, carried between neighbours by the real conductivity of their materials and
along heat pipes, and lost to the place around them: to the air as thick as it is, and by radiation, which in a
vacuum is the only way out. It is in the spirit of Mekanism's heat, with real physics and real data.

The design behind this page is [Heat](design/heat.md).

## Real units

*Added 2026-10-15.*

Everything is in real units: kelvin, joules, watts, seconds. A block is a cube of one metre; two blocks face to face
share a square metre, their centres a metre apart. `HeatMath` holds the physics, pure and tested:

| What | How |
| --- | --- |
| Heat capacity | mass in kilograms times the material's specific heat, in joules a kelvin |
| Conduction through a face | `2 k1 k2 / (k1 + k2)` watts a kelvin, half a metre of each material in turn |
| Convection to the air | 10 W/(m²·K) in the overworld's air, in proportion to the air's pressure: none in a vacuum |
| Radiation | `e s A (T⁴ - Te⁴)`, Stefan and Boltzmann's law, in air and in a vacuum alike |

No step carries heat past the temperature the two sides would share, so a long step on a small body is as stable as
a short one on a large body.

## What materials do with heat

*Added 2026-10-15.*

Every material of the catalogue carries its density (kg/m³), specific heat (J/(kg·K)), thermal conductivity
(W/(m·K)), emissivity (0 to 1) and melting and boiling points (K) as properties (`Thermal.DENSITY` and the others),
which a mod gives its materials and a data pack may change. The elements of every day carry their real values; a
material that gives none is taken as its kind is on the whole (a metal as iron, a mineral as stone, a gas as air).

```java
Thermal copper = Thermal.of(Elements.of(Element.COPPER), false);   // 8,960 kg/m³, 385 J/(kg·K), 401 W/(m·K)
Materials.material(id("bronze"), MaterialKind.METAL, NAME).with(Thermal.CONDUCTIVITY, 50.0)...;
```

*Added 2026-10-15:* a material that is boiled carries three more: its heat of boiling (`Thermal.LATENT_HEAT`, J/kg),
the mass of a mole of its gas's molecules (`Thermal.MOLAR_MASS`, kg/mol: two atoms of nitrogen to a molecule) and,
for a gas at room temperature, how dense its liquid is (`Thermal.LIQUID_DENSITY`). Water and the liquefied gases
(hydrogen, helium, nitrogen, oxygen, the noble gases, fluorine, chlorine) carry their real values, as do bromine and
mercury. From them:

| Method | What it gives |
| --- | --- |
| `boilingAt(kiloPascals)` | where it boils under that pressure, by Clausius and Clapeyron: water at 71 °C on top of Everest, at about −5 °C on Mars, at 179 °C at ten bar |
| `phaseAt(kelvin, kiloPascals)` | solid, liquid or gas; where the air is so thin that it would boil below its melting point, it goes from solid to gas |
| `heatOfBoiling()` | its own, or Trouton's rule (88 J/(mol·K) at its boiling point) over its molar mass |
| `litreKilograms()` | what a millibucket, a litre, of its liquid weighs |

A gas is counted as gases are counted everywhere in the Core: at 0 °C and the open air's pressure, a mole to 22.4
millibuckets.

## Liquids and their materials

*Added 2026-10-15.*

A material's liquid registered by the Core knows its material. A liquid of the game or of another mod is linked to
one, so heat knows what it is: the game's water is the series' water (`SharedCatalogue.WATER`, H2O), whose gas is
steam and whose solid is ice.

```java
MaterialLiquids.link(WATER, ResourceLocation.withDefaultNamespace("water"));
MaterialLiquids.materialOf(Fluids.WATER);   // the series' water
```

A liquid's temperature is its fluid type's: the game's water at 300 K, its lava at 1,300 K, a material's liquid at
what its mods said (`FluidHeat.temperature`).

## A block that holds heat

*Added 2026-10-15.*

A block entity holds heat in a `HeatBody`: so many cubic metres of a material, and its temperature. It implements
`IHeatHolder` and keeps the body in its constructor; the Core offers it to its neighbours and heat lines on the
`HeatPorts.HEAT` capability, with nothing to register.

```java
public final class RadiatorBlockEntity extends SyncedBlockEntity implements IHeatHolder {

    private final HeatBody heat = HeatBody.of(Thermal.of(copper, false), 0.05).exposed(10.0);   // 10 m² of fins

    public RadiatorBlockEntity(final BlockPos pos, final BlockState state) {
        super(TYPE.get(), pos, state);
        HeatPorts.keep(this, heat);   // saved, sent to the players, woken as it changes
    }

    @Override
    public HeatBody heatBody() {
        return heat;
    }
}
```

A body starts at the temperature of the place it stands in. `exchange(joules)` puts heat in or takes it out, and
`exposed(squareMetres)` gives it surface besides its faces, as a radiator's fins do.

Other mods' blocks join by offering `IHeatPort` on the same capability: their temperature, their heat capacity,
their face's conductivity, and heat put in or taken out.

## The place around it

*Added 2026-10-15.*

Each face of a block that holds heat meets one of three things:

- **another block that holds heat**: heat flows through the face by the two materials' conductivity;
- **the air, a liquid or anything that is not solid**: the face gives heat to the air by convection, as thick as the
  air is, and radiates;
- **a solid block of the world**: heat flows into it as that block conducts (below), the world's blocks holding the
  place's temperature.

The place's temperature and air pressure come from the dimension's rules (`HeatClimate`); in a vacuum (below
0.1 kPa) nothing carries heat off but radiation, so a ship or a base in space needs radiators. The biome's climate,
the altitude and a sealed room's air come to it as each is built.

*Added 2026-10-15:* a place's own air comes from the sources declared for it, the first that speaks of a place
giving its temperature and pressure, and the dimension's rules where none does. A sealed room is one; a cold store a
mod builds is another.

```java
HeatClimate.source((level, pos) -> coldStore.contains(pos) ? new HeatClimate.Air(243.15, 101.3) : null);
```

## Insulation

*Added 2026-10-15.*

How well each block of the world conducts heat is a data map of blocks, `jscore:thermal_conductivity`, in watts a
metre a kelvin, which a mod or a data pack fills (`BlockConductivity`). The Core gives the game's own blocks near
their real values; a block it says nothing of conducts as stone, 2.5.

| Blocks | W/(m·K) |
| --- | --- |
| wool | 0.04 |
| hay | 0.07 |
| planks and logs | 0.15 |
| snow | 0.16 |
| sand, dirt, gravel | 0.3, 0.5, 0.7 |
| bricks, terracotta, glass | 0.7, 0.8, 1.0 |
| clay, obsidian, concrete, sandstone, basalt | 1.3 to 2.0 |
| ice | 2.2 |
| iron, gold, copper, diamond blocks | 80, 318, 401, 2,200 |

A hot block walled in wool keeps its heat; walled in stone it loses it, and against iron faster still. A mod's
insulation, rock wool or aerogel, adds itself:

```json
{ "values": { "mymod:rock_wool": 0.04, "mymod:aerogel": 0.015 } }
```

A pipe is insulated against the cold by its cable's `CableType.Builder.insulated()`.

## Pipes that freeze

*Added 2026-10-15.*

A pipe of a liquid freezes where the place is colder than the liquid's melting point, and the part of the grid it is
in carries none of that liquid until the place warms or the pipe is changed for an insulated one. Only liquids at
room temperature freeze so, water first; a molten metal or a liquefied gas is held by pipes made for it. It is a
setting of consequence: `heat.pipes_freeze` in the balance file turns it off.

## Heat pipes

*Added 2026-10-15.*

A line of the cable block on the `GridKind.HEAT` grid is a heat pipe: it plugs into any block that holds heat, and
carries heat between all it plugs into at once, almost whatever its length, as a real heat pipe does. Its cable's
throughput is read as its conductance in watts a kelvin:

```java
CableType.builder(Connection.of(id("heat_pipe"))).grid(GridKind.HEAT).alone().carries(200, 0)...
```

Each part of the grid is a node that holds no heat: it stands at the temperature where what flows in equals what
flows out, so nothing is made or lost.

## Sources and sinks

*Added 2026-10-15.*

A burner, an electric heater, a reactor's core or a cooler is a `HeatSource`: a power and a limit, the hottest a
heater makes its body or the coldest a cooler does, where it stops as a thermostat holds it. The block calls it from
its tick; it says the joules it put in, which an electric heater takes from its energy joule for joule.

```java
private static final HeatSource HEATER = HeatSource.heater(5_000, 900);   // 5 kW, up to 900 K
private static final HeatSource CHILLER = HeatSource.cooler(2_000, 250);   // takes 2 kW out, down to 250 K
...
double joules = HEATER.apply(heat, HeatMath.TICK_SECONDS);   // in its tick
```

A radiator is a body with fins (`exposed`), and the place around a block is the sink every block has.

## Boilers

*Added 2026-10-15.*

A `Boiler` boils the liquid in a tank into its gas in a tank of gas by the heat of a block's wall, with the real
enthalpy: each millibucket, a litre, takes the heat that brings it from its own temperature to where it boils, then
its heat of boiling. A litre of the game's water at 300 K takes 2.56 MJ (0.998 kg warmed by 73 K, then 2.257 MJ a
kilogram) and makes about 1,240 millibuckets of steam.

```java
private final Boiler boiler = Boiler.of(50_000);   // its tubes pass 50 kW a kelvin
...
fields().part("Boiler", boiler).save();   // in the constructor: the heat of the litre now boiling is kept
...
boiler.boil(level, pos, heat, waterTank, steamTank, HeatMath.TICK_SECONDS);   // in its tick
```

The heat that passes in a step is the tubes' conductance times how far the wall is above the boiling point, and never
takes the wall below it. Where it boils is the pressure's: the place's air, so water boils lower on Mars, or the
vessel's own with `Boiler.at(kiloPascals)`, as a pressure vessel boils it higher. Any liquid of a material with a gas,
a heat of boiling and a molar mass boils so; liquid nitrogen in a warm wall boils as water does in a hot one.

## Exchangers

*Added 2026-10-15.*

A loop that carries heat has its liquid in two forms, a fluid each, at their own temperatures: the oil of a solar
trough, cool coming back from the boiler and hot going to it. A mod declares the pair once:

```java
HeatForms.declare(id("thermal_oil"), id("hot_thermal_oil"));
```

A `FluidPass` passes the liquid in one tank through a block's wall into the other form in another tank: a hot form
comes out cool where the wall is colder than the cool form, and the wall warms by what it gave; a cool form comes out
hot where the wall is hotter than the hot form, and the wall cools by what it took. A millibucket's change is worth
its mass times its specific heat times the kelvin between the forms. Two passes through one wall are a heat exchanger
between two liquids, the wall between them; a pass beside a boiler is the boiler of a solar trough or of a reactor's
loop.

```java
private final FluidPass pass = FluidPass.of(10_000);   // 10 kW a kelvin of wall
...
pass.run(heat, hotOilIn, coolOilOut, HeatMath.TICK_SECONDS);
```

For sizing an exchanger by hand, `HeatExchanger` gives the effectiveness method engineers use: the watts between two
streams running against each other (`counterflow`), and between a wall and a stream (`wall`), neither stream ever
coming out past where the other went in.

## Cold liquids boil off

*Added 2026-10-15.*

A liquid kept colder than the place it is in, liquid nitrogen or liquid hydrogen, sits at its boiling point and boils
off as the place's heat leaks into its tank. A `BoilOff` is the tank's insulation, in watts a square metre a kelvin,
over its surface: the leak is that times how much warmer the place is than where the liquid boils at the place's
pressure, and each millibucket that boils off takes its heat of boiling.

| Insulation | W/(m²·K) | A cubic metre of liquid nitrogen in the overworld lasts |
| --- | --- | --- |
| `BoilOff.BARE` | 10 | three and a half hours |
| `BoilOff.INSULATED` | 0.5 | three days |
| `BoilOff.VACUUM_JACKETED` | 0.02 | over two months |

```java
private final BoilOff boilOff = BoilOff.of(BoilOff.INSULATED, 6.0);   // a cubic metre's six square metres
...
fields().part("BoilOff", boilOff).save();
...
boilOff.vent(level, pos, tank, gasTank, HeatMath.TICK_SECONDS);   // its gas into a tank, or the air with null
```

What boils off goes into the block's tank of gas while it has room, and into the air beyond. It is a setting of
consequence: `heat.boil_off` in the balance file turns it off.

## It costs nothing at rest

*Added 2026-10-15.*

Only blocks that hold heat take part, and only while they are out of balance (`LevelHeat`): each is looked at in
turn, at most `LevelHeat.BATCH` a tick, for the seconds since it was last looked at. A block within `LevelHeat.BALANCE`
of everything round it sleeps until something changes it: heat put in, a neighbour placed or broken, its chunk
loaded.

## What can go wrong

- **A block never warms.** Its neighbour holds no heat (it is a block of the world), or the heat it gets leaves as
  fast through its other faces: a litre of metal warms in seconds, a cubic metre in hours, as in the world.
- **A heat pipe carries nothing.** It plugs into fewer than two blocks that hold heat, or its end is set off.
- **A boiler boils nothing.** Its wall is no hotter than where the liquid boils at its pressure, its gas tank is full
  or holds another gas, or the liquid is of no material, or of one with no gas, no heat of boiling or no molar mass:
  link another mod's liquid to its material with `MaterialLiquids.link`.
- **An exchanger passes nothing.** The liquid has no other form declared, the wall cannot take it to that form's
  temperature (a hot form needs a wall colder than its cool form, a cool form one hotter than its hot form), or the
  tank it goes to is full.
- **Water stops in a pipe.** A pipe of the part runs where the place is below 0 °C: insulate it, warm the place, or
  turn `heat.pipes_freeze` off.
- **A wall of a mod's insulation does nothing.** The block is not in the `jscore:thermal_conductivity` data map, so
  it conducts as stone.
