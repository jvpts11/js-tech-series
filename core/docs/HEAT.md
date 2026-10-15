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

A body starts at the temperature of the place it stands in. `exchange(joules)` puts heat in or takes it out: a
burner, an electric heater, a cooler each call it with their power times the time. `exposed(squareMetres)` gives it
surface besides its faces, as a radiator's fins do.

Other mods' blocks join by offering `IHeatPort` on the same capability: their temperature, their heat capacity,
their face's conductivity, and heat put in or taken out.

## The place around it

*Added 2026-10-15.*

Each face of a block that holds heat meets one of three things:

- **another block that holds heat**: heat flows through the face by the two materials' conductivity;
- **the air, a liquid or anything that is not solid**: the face gives heat to the air by convection, as thick as the
  air is, and radiates;
- **a solid block of the world**: heat flows into it, the world's blocks holding the place's temperature, at stone's
  conductivity.

The place's temperature and air pressure come from the dimension's rules (`HeatClimate`); in a vacuum (below
0.1 kPa) nothing carries heat off but radiation, so a ship or a base in space needs radiators. The biome's climate,
the altitude and a sealed room's air come to it as each is built.

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
