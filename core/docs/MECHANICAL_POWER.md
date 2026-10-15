# Mechanical power

Rotation carried by shafts, gears and gearboxes, from water wheels, windmills, steam engines, hand cranks and motors
to the machines that turn with it, in real units: speed in revolutions a minute, torque in newton-metres, power in
watts. It is in the spirit of Create's kinetic system, with real physics. The kit is the Core's; the blocks, their
models and their recipes are each mod's.

The design behind this page is [Mechanical power](design/mechanical-power.md).

## Real units

*Added 2026-10-15.*

`RotationMath` holds the physics, pure and tested:

| What | How |
| --- | --- |
| Power | torque times angular speed: 1,000 N·m at 60 rpm is 6.28 kW |
| Two gears in mesh | the driven one turns the other way, slower by as many times as it has more teeth, with that much more torque |
| A network's inertia | each part's, times the square of the ratio it turns at |
| A step | faster or slower by the net torque over the inertia, never past the speed where the torque balances |

So a light line of shafts settles at its speed at once, and a heavy flywheel takes its time; neither swings about
the balance.

## A network

*Added 2026-10-15.*

Parts joined face to face turn as one network (`ShaftNetwork`), each at its ratio to the network's speed. Each face
of a part meets its neighbour in one of three ways (`ShaftContact`):

- **an axle** along the face's axis, which turns the neighbour's axle with it;
- **the teeth of a gear**, which mesh with a gear beside it on a parallel axis and turn it the other way, by the
  ratio of their teeth;
- **not at all**.

A network whose ratios disagree, two meshes on the same pair of shafts with different gears, is **jammed** and does
not turn. Each tick the network feels what its sources drive, less what its loads ask, less the friction of every
part, and turns faster or slower. **Too little power slows it down instead of stopping it dead**: it turns where
the torque its sources give meets the torque its loads ask.

Networks are worked out again only when a part is placed, broken, loaded or changes how it meets its neighbours
(`LevelMechanics`), and turned once a tick each, never shaft by shaft. A part is told its speed only when it changes:
a network at its speed costs no more than the torque of its sources and loads, and one with neither, standing
still, costs nothing.

## Sources and their curves

*Added 2026-10-15.*

A source declares its curve (`TorqueCurve`): its full torque from a standstill, falling evenly to nothing at its free
speed, as a water wheel's or an electric motor's does, and never more than its power allows.

```java
TorqueCurve crank = TorqueCurve.of(20, 75, 60);          // a man's arm: 20 N·m, 75 W, up to 60 rpm
TorqueCurve wheel = TorqueCurve.of(4_000, 15_000, 12);   // a water wheel: slow and strong
```

A crank of 20 N·m against a load of 15 turns at 15 rpm, where the falling torque meets the load; with a gear of 16
teeth driving one of 32, a load of 30 on the large gear is 15 at the crank, and the same balance.

## The parts of the kit

*Added 2026-10-15.*

A part of the kit is a `MechanicalPartBlock`, set along the axis of the face it is placed against, with what it is
(`MechanicalPartSpec`), its block entity a `MechanicalPartBlockEntity` of a type its mod registers:

| Part | Faces |
| --- | --- |
| `shaft(inertia, friction)` | an axle at each end |
| `gear(teeth, inertia, friction)` | an axle at each end, and its teeth on the four faces around its axis |
| `gearbox(inertia, friction)` | an axle on each of the four faces around its axis, opposite faces turning opposite ways |

```java
public static final BlockEntry<MechanicalPartBlock> IRON_SHAFT = CONTENT.block("iron_shaft",
        properties -> new MechanicalPartBlock(properties, MechanicalPartSpec.shaft(0.005, 0.1), () -> PART.get()))
        .named("Iron Shaft").register();
public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalPartBlockEntity>> PART =
        CONTENT.blockEntity("part", (pos, state) -> new MechanicalPartBlockEntity(PART.get(), pos, state),
                IRON_SHAFT, BRASS_GEAR, GEARBOX);
```

An iron shaft a metre long and five centimetres thick weighs about 0.005 kg·m² in turning and holds back about
0.1 N·m in its bearings.

A part of a mod's own (a water wheel, a windmill, a machine that turns) implements `IMechanicalPart`: how each face
meets its neighbour, its inertia and friction, what it drives or asks at each of its own speeds (`ITorque`: positive
drives, a load asks against the way it turns), and its speed as the network tells it. It calls
`MechanicalPorts.keep(this)` from its constructor, and the Core offers it on `MechanicalPorts.MECHANICAL`.

## Motors and dynamos

*Added 2026-10-15.*

A machine of the Core's kit turns into a motor or a dynamo by declaring its shaft (`MachineShaft`,
[Machines](MACHINES.md)) on one of its faces:

```java
turnsWith(MachineShaft.motor(TorqueCurve.of(50, 5_000, 1_500), 0.9, RelativeFace.BACK));   // in the constructor
...
protected MachineStatus work(ServerLevel level) {
    return shaft().run();
}
```

A **motor** turns the network by its curve and spends the power it gives, over its efficiency, from the machine's
energy, whatever energy that is, a joule to an FE. A **dynamo** brakes the network the harder the faster it turns, up
to its curve at its rated speed, and makes energy of what it takes, at its efficiency, while the machine has room. A
stopped machine neither drives nor takes.

## What can go wrong

- **Nothing turns.** The network is jammed (its ratios disagree), its sources give less than its friction, or a face
  meets nothing: a shaft turns only its neighbours along its axis, and gears mesh only on parallel axes.
- **It turns slower than the source's free speed.** It is meant to: it turns where the torque meets what it is asked.
- **A motor spends nothing.** It stands still against a load stronger than its stall torque, which takes no power.
