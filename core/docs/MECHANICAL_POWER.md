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

*Added 2026-10-15:* the rest of the kit, each a shaft along its axis but for what it does:

| Part | What it does |
| --- | --- |
| `flywheel(inertia, friction)` | weighs much in turning: it smooths its network, takes time to start, and keeps turning after the power is cut |
| `clutch(inertia, friction)` | lets go while redstone powers it, splitting the line in two |
| `brake(newtonMetres, inertia, friction)` | holds back as much as the redstone it gets says, up to its braking torque at 15 |
| `controller(inertia, friction)` | its end towards the positive end of its axis turns up to twice as fast as the other, as its redstone says (15 is twice), and lets go with none |
| `shearPin(newtonMetres, inertia, friction)` | shears when the torque through it passes its rating, stopping the line and sparing everything else, as a fuse does; using it pins it again |
| `pulley(metres, inertia, friction)` | a belt or a chain joins it to another pulley across a distance |
| `torqueMeter(inertia, friction)` | measures the torque through it, for sensors and Jade |

Any part can carry a rating (`spec.rated(newtonMetres)`): when the torque through it passes the rating, it breaks
and drops, unless the server turned that off (`mechanical.parts_break` in the balance file). The torque through a
part can be told only where every path of its network crosses it; there it is what the parts on one side of it
drive and ask, less their friction, less what it takes to speed them up.

## Belts and chains

*Added 2026-10-15.*

A `ShaftLinkItem` is a belt or a chain: used on one pulley, then on another turning on the same axis beside it,
within its reach, it joins them, and the two turn the same way, the smaller the faster (a pulley of 0.1 m turns
twice as fast as one of 0.2 m). A belt slips a little and holds back 0.5 N·m on each pulley, a chain 0.2. Breaking
either pulley drops the belt and frees the other.

```java
ItemEntry<ShaftLinkItem> BELT = CONTENT.item("belt", properties -> new ShaftLinkItem(properties, false, 16))
        .named("Belt").register();   // a belt reaching 16 blocks; true for a chain
```

A part of a mod's own joins across a distance by saying its `joins()`: each the other part's place and the ratio it
turns at. A join only one of the two says is none.

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

*Added 2026-10-15:* a machine can also be **driven** by its shaft, as a mill turned by a water wheel is:

```java
turnsWith(MachineShaft.driven(10, 20, 60, RelativeFace.BACK));   // asks 10 N·m; stops below 20 rpm; whole at 60
```

While it has work it asks its torque of the network, and works as fast as its shaft turns against its rated speed;
below its least speed it stops, `TOO_SLOW`, still asking, so a source too weak for it holds it there until a
stronger one turns it. A processing machine of the kit does all this by itself.

## Reading it

*Added 2026-10-15.*

- **Every part of the kit is a sensor of its speed**, as a tachometer, offered to whatever reads sensors and to a
  comparator: no signal at a standstill, the full 15 at 3,000 rpm (`MechanicalReadings.tachometer(rpm, most)`).
- **A torque meter** (`MechanicalPartSpec.torqueMeter`) also reads the torque through it, up to the full 15 at
  1,000 N·m, where every path of its network crosses it (`MechanicalReadings.torqueMeter(newtonMetres, most)`).
- **Jade** shows how fast a part turns, a torque meter's torque, and its network's power, or that it is jammed.
- **The network probe**, used on any part that turns, reads its network of shafts on its own pages, in watts: the
  power its sources give, its sources and its loads, and what friction takes.

## Seen and heard turning

*Added 2026-10-16.*

A part's speed is sent to the players who see it, and a part made with `MechanicalPartBlock.turning(...)` is drawn
turning at that speed by a model of the Core's ([Models](MODELS.md)) in place of its block's own model. Its block
hides nothing beside it and lets light through, so the faces around it are drawn and the part is lit as its place
is.

```java
BlockEntry<MechanicalPartBlock> GEAR = CONTENT.block("gear", properties -> MechanicalPartBlock.turning(properties,
        MechanicalPartSpec.gear(16, 0.05, 0.2), () -> PART.get())).named("Gear").register();

// From the client setup, and in EntityRenderersEvent.RegisterRenderers:
TurningParts.draw(MyParts.GEAR, rl("gear"));                          // the whole model turns about its axle
TurningParts.draw(MyParts.GEARBOX, rl("gearbox"), "input", "output"); // only these bones turn
TurningParts.register(event, MyParts.PART.get());
```

- **A model of a turning part is made standing up**, its axle along Y through the middle of its block; it is laid
  along the part's axis as the part is placed.
- **The angle follows the speed** frame by frame, so a part speeding up or slowing down does so smoothly, whatever
  the server's ticks. Every other gear stands half a tooth on, so two meshed gears' teeth meet.
- **Every part drawn with one model shares its shape**: the model is baked once and every part only turns it.
- **A belt or a chain runs** between the pulleys it joins, as fast as the pulleys' rims: its two straight runs touch
  both pulleys as an open belt does (`BeltPath`), drawn with the game's black wool for a belt and its chain for a
  chain, as thick as a belt is. A pulley's model is made as round as its spec's radius, so the belt meets its rim.
- **A part is heard turning** when its block declares a sound: `turningSound(loop, fullRpm)` plays its looping sound
  while it turns, louder and higher the faster, as loud as it gets and a half higher than recorded at `fullRpm`, and
  silent below a fiftieth of that.

## What can go wrong

- **Nothing turns.** The network is jammed (its ratios disagree), its sources give less than its friction, or a face
  meets nothing: a shaft turns only its neighbours along its axis, and gears mesh only on parallel axes.
- **It turns slower than the source's free speed.** It is meant to: it turns where the torque meets what it is asked.
- **A motor spends nothing.** It stands still against a load stronger than its stall torque, which takes no power.
- **A machine stays `TOO_SLOW`.** Its source cannot turn it at its least speed against the torque it asks: give it a
  stronger source, or gear the line so the source turns faster where the machine is.
- **A pin shears again at once.** The load beyond it still asks more than its rating: take load off, or fit a
  stronger pin.
- **Two pulleys will not join.** They must turn on one axis, side by side (no offset along the axis), within the
  belt's reach, and neither may be joined already.
- **A part turns but is drawn as its block, or not at all.** It is drawn turning only when its block was made with
  `MechanicalPartBlock.turning(...)` and its mod named its model with `TurningParts.draw(...)` and registered the
  renderer of its block entity type with `TurningParts.register(...)`; a model not found draws nothing.
- **The belt floats off the pulleys.** The pulleys' models are not as round as their specs' radii.
