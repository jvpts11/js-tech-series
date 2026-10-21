# Entities

Things in the world that move: creatures, vehicles, robots, shots. J's Core declares them like blocks, and gives
three kinds ready to build on: vehicles a player drives, robots that work a list of tasks, and projectiles. It
also finds what a player or a creature is wearing, wherever it is worn.

## Declaring an entity

*Added 2026-10-05.*

```java
public static final EntityEntry<Helper> HELPER = CONTENT.entity("helper", Helper::new, MobCategory.MISC)
        .named("Helper")
        .size(0.6F, 1.2F)                            // width and height in blocks
        .attributes(RobotEntity::robotAttributes)    // its health and speed
        .register();
```

| Step | What it does | Default |
| --- | --- | --- |
| `named("...")` | The English name. Required. | |
| `size(width, height)` | Its box, in blocks. | 0.6 by 1.8, a player's |
| `tracking(chunks, everyTicks)` | How far away players see it, and how often it is sent to them. | 8 chunks, every 3 ticks |
| `fireImmune()` | Fire and lava do not hurt it. | |
| `attributes(...)` | Health, speed and the like, for a living entity. | |
| `holdsEnergy()` | Its energy is offered to chargers and cables, for an entity that implements `IEnergyHolder`. | |

What it gives back is NeoForge's `DeferredHolder` of the entity type. Its look (a renderer and a model) is your
mod's, registered on the client as for any entity.

## Vehicles

*Added 2026-10-05; rewritten 2026-10-21.*

A vehicle is something players and robots sit in and drive: a car, a van, a lorry and its trailer, a boat, a
drone. Every vehicle is the one entity `jscore:vehicle`, made of a **kind** that says what it is and of the
**parts** fitted in it, which say what it does. A kind and its parts are data: a mod declares them in code, and a
data pack adds or changes them with files, with no code at all. A file wins over a declaration of the same id.

| What | Declared with | Data pack folder |
| --- | --- | --- |
| A kind of vehicle | `Vehicles.declare(id, VehicleDef)` | `data/<namespace>/jstech/vehicles/` |
| A part | `VehicleParts.declare(id, VehiclePartDef)` | `data/<namespace>/jstech/vehicle_parts/` |
| A fuel | `Fuels.declare(id, Fuel)` | `data/<namespace>/jstech/fuels/` |
| A seat's role | `VehicleRoles.declare(id, name, drives)` | (code only) |

The game's creative menu lists a vehicle item for every kind and a part item for every part, under tools and
utilities. Use the item on the ground to place the vehicle, facing the way you look.

### Places, in metres

Everything in a vehicle is placed in metres from the middle of its underside: `x` to its **left**, `y` up, `z`
**ahead**. A seat at `[0.4, 0.5, 0.4]` is the left front seat of a car; a wheel at `[-0.7, 0, -0.9]` is the right
rear wheel.

### A kind of vehicle

```java
public static final ResourceLocation VAN = Vehicles.declare(rl("van"), VehicleDef.builder("vehicle.mymod.van")
        .model(rl("van"))                                  // a model of the Core's models (see Models)
        .body(1.8F, 1.6F, 1_500, 0.9)                      // width, height (blocks), mass (kg), drag area (m²)
        .seat(new Vec3(0.4, 0.5, 0.4), VehicleRoles.DRIVER)
        .seat(new Vec3(-0.4, 0.5, 0.4), VehicleRoles.PASSENGER)
        .place("engine", VehiclePartKind.ENGINE, new Vec3(0, 0.5, 0.9), ENGINE)
        .place("gearbox", VehiclePartKind.GEARBOX, new Vec3(0, 0.3, 0.5), GEARBOX)
        .place("tank", VehiclePartKind.TANK, new Vec3(0, 0.3, -0.6), TANK)
        .place("headlights", VehiclePartKind.LIGHT, new Vec3(0, 0.6, 1.0), HEADLIGHT)
        .wheel("front_left", new Vec3(0.7, 0, 0.9), WHEEL, true, false)     // steers
        .wheel("front_right", new Vec3(-0.7, 0, 0.9), WHEEL, true, false)
        .wheel("rear_left", new Vec3(0.7, 0, -0.9), WHEEL, false, true)     // driven
        .wheel("rear_right", new Vec3(-0.7, 0, -0.9), WHEEL, false, true)
        .boot(9)                                           // slots
        .cargo(4_000, FluidTags.WATER)                     // a cargo tank, in millibuckets, and what it holds
        .paint("white", "paint.mymod.white", rl("textures/entity/van_white.png"), DyeColor.WHITE)
        .paint("red", "paint.mymod.red", rl("textures/entity/van_red.png"), DyeColor.RED)
        .tow(new Vec3(0, 0.4, -1.1))                       // where a trailer hitches on behind
        .key(rl("van_key"))                                // the item cut as its key
        .door("door_left")                                 // a bone of its model that swings open
        .build());
```

The same kind as a file, `data/mymod/jstech/vehicles/van.json`:

```json
{
  "name": "vehicle.mymod.van",
  "model": "mymod:van",
  "movement": "road",
  "body": {"width": 1.8, "height": 1.6, "mass": 1500, "drag_area": 0.9},
  "seats": [
    {"at": [0.4, 0.5, 0.4], "role": "jscore:driver"},
    {"at": [-0.4, 0.5, 0.4]}
  ],
  "places": [
    {"name": "engine", "kind": "engine", "at": [0, 0.5, 0.9], "part": "mymod:diesel_engine"},
    {"name": "tank", "kind": "tank", "at": [0, 0.3, -0.6], "part": "mymod:steel_tank"},
    {"name": "front_left", "kind": "wheel", "at": [0.7, 0, 0.9], "part": "mymod:road_wheel", "steers": true},
    {"name": "rear_left", "kind": "wheel", "at": [0.7, 0, -0.9], "part": "mymod:road_wheel", "driven": true}
  ],
  "boot": 9,
  "cargo": [{"capacity": 4000, "holds": "minecraft:water"}],
  "paints": [{"id": "white", "name": "paint.mymod.white", "texture": "mymod:textures/entity/van_white.png",
              "dye": "white"}],
  "hitching": {"tow": [0, 0.4, -1.1]},
  "key": "mymod:van_key",
  "doors": ["door_left"]
}
```

| Field | What it is | Default |
| --- | --- | --- |
| `name` | The translation key of its name. Required. | |
| `model` | The model it is drawn with. | none: drawn as nothing |
| `movement` | `road` on its wheels, `air` held up by its rotors, `water` afloat. | `road` |
| `body` | `width` and `height` of its box in blocks, `mass` in kilograms, `drag_area` in square metres. | drag area 0.8 |
| `seats` | Each seat's place and role. A kind has at least one seat. | role `jscore:passenger` |
| `places` | Where each part goes: its `name`, its `kind`, its place, the part it is made with, and for a wheel whether it `steers` and is `driven`. | made empty |
| `boot` | How many slots its boot has, up to 54. | 0 |
| `cargo` | Its cargo tanks: `capacity` in millibuckets, and the fluid tag it `holds`. | any fluid |
| `paints` | The paints it can wear, the first the one it is made in: a texture for its model and the dye that paints it. | |
| `variants` | The shapes it comes in, each a model of its own (a van with a high roof); the first is the plain one. | |
| `hitching` | `hitch`, where it hitches to a vehicle ahead (a trailer), and `tow`, where one hitches on behind. | none |
| `key` | The item cut as its key (see below). | driven by whom its owner lets |
| `icon` | An item drawn for it while it is an item. | its model, shrunk |
| `doors` | Its doors, by the bones of its model that swing open; up to sixteen. | |

### Parts

A part is one of nine kinds, and carries what its kind needs:

| Kind | What it does | Its numbers |
| --- | --- | --- |
| `engine` | Drives it, burning one of its fuels, or drawing on its battery when electric. | `power` in watts, `idle_rpm`, `max_rpm`, `efficiency` (the share of what it burns it turns into work), `electric`, `fuels`, `sound` |
| `wheel` | Carries it on the ground; the driven ones push it, the steering ones turn it. | `radius` in metres, `grip` (1 a road tyre's), `stiffness` and `damping` of its spring, `travel` |
| `tank` | Holds its fuel, a liquid or a gas pressed in. | `capacity` in millibuckets |
| `battery` | Holds charge for an electric engine. | `capacity` in joules (1 FE is 1 J) |
| `gearbox` | Its gears; without one it has a single gear. | `ratios`, `reverse`, `final_drive` |
| `light` | A headlight, a tail light, a brake light, an indicator or a light inside. | `role`, `level` (1 to 15), `colour` (`#RRGGBB`), `reach` in blocks |
| `horn` | Sounds when the driver presses the horn key. | `sound` |
| `rotor` | Lifts a vehicle that flies. | `thrust` in newtons |
| `propeller` | Pushes a vehicle afloat. | `thrust` in newtons |

Every part also has a `name` (its translation key), an `icon` (an item drawn for it), the `repair` item that mends
it, and its `mass` in kilograms, which adds to the vehicle's.

```java
public static final ResourceLocation ENGINE = VehicleParts.declare(rl("diesel_engine"), VehiclePartDef.engine(
        "vehicle_part.mymod.diesel_engine", 150, new VehiclePartDef.Engine(60_000, 800, 4_000, 0.3, false,
                List.of(DIESEL), Optional.of(rl("diesel_running")))).mendedWith(rl("minecraft", "iron_ingot")));
```

### Fuels

A fuel is a liquid, by a fluid tag, or a gas, by the materials it is made of, and what a millibucket of it gives in
joules. An engine burns only the fuels it names, and a vehicle's fuel tank takes only those.

```java
public static final ResourceLocation DIESEL = Fuels.declare(rl("diesel"),
        Fuel.liquid("fuel.mymod.diesel", MyTags.DIESEL, 36_000_000));            // joules a millibucket
public static final ResourceLocation METHANE = Fuels.declare(rl("methane"),
        Fuel.gas("fuel.mymod.methane", List.of(rl("methane")), 36_000));
```

As a file, `data/mymod/jstech/fuels/diesel.json`:
`{"name": "fuel.mymod.diesel", "liquid": "mymod:diesel", "joules_per_mb": 36000000}`.

### How it goes

The one in a driving seat drives it with the walking keys: forward speeds it up, back brakes it and then backs it
up, left and right steer. A vehicle that flies climbs with jump and sinks with sprint. Its physics are real ones,
in real units:

- **On the road**, its engine's power reaches the driven wheels through its gearbox, changing gear by itself
  unless the driver chose a manual gearbox. What the wheels can push is held to what their tyres grip: less on
  ice, a tenth as much. It leans as its wheels stand on the ground, slides wide past what its grip holds in a fast
  turn, climbs a slope slower and runs down it faster, and steps up a block with wheels big enough.
- **In the air**, its rotors hold it up while their thrust beats its weight; it climbs and sinks at most four metres
  a second, and forward pushes it ahead.
- **Afloat**, the water bears it up and its propellers push it; its rudder turns it only while it moves.
- The air thins with height, and the pull of each dimension is its own ([World](WORLD.md)).

A player's own game drives the vehicle they sit in, as a boat is driven, so it answers at once however far the
server is; the server works out what its engine burns from where it went. A robot drives it with its `Drive` task.

The driver's other keys, each one a player can change in the game's controls: **lights** (G), cycling side lights,
dipped and main beams; the **horn** (H); and, with no key until given one, the **ignition**, the **next seat**,
**shift up** and **shift down** for a manual gearbox, **automatic or manual gearbox**, and the **indicators**, left
and right, either with sneak held for the hazard lights. The lights light the world around them, the headlights far
ahead. An engine that names a sound sounds it, faster and louder as it turns and works.

### Seats, owners and keys

Each seat has a role. A role that drives (the Core's `jscore:driver`, or one a mod declares) is for whoever may
drive the vehicle; any other is for anyone. Getting in, a player or a robot takes a driving seat when they may
drive and one is free, and another seat otherwise. The **next seat** key moves to the next free seat they may sit
in. A creature that is not a robot never drives.

A vehicle placed by a player is that player's, through the Core's ownership: they, and those they let use it, may
drive it. Its kind may name a **key**, an item a mod declares as a `VehicleKeyItem`:

```java
CONTENT.item("van_key", properties -> new VehicleKeyItem(properties.stacksTo(1))).named("Van Key").register();
```

A vehicle placed from its item gives its owner a key. Whoever carries a key cut for it may drive it, and the key
used on it with sneak held locks and unlocks it; a locked vehicle lets in only who may drive it.
`vehicle.forgetKeys()` cuts its keys anew: every key cut before opens nothing.

### Its parts, its paint, its wear

Used on a vehicle, a part's item goes into the nearest empty place of its kind. A wrench used with sneak held takes
out the part nearest where you aim, into your hands; a wrench with the part's repair item in the other hand mends
the worn part nearest by a quarter. A dye paints it the next paint of that colour. Only whom its owner lets set
things up changes its parts and paint.

Its parts wear:

- **A blow.** Running into a wall faster than four metres a second wears the parts on the side that struck, by the
  energy of the blow, most where it struck. Other harm (fire, explosions) wears every part.
- **A hard landing.** Landing faster than eight metres a second wears its wheels.
- **Heat.** An engine run above 120 °C wears itself.
- **A holed tank.** A tank worn below a third leaks what it holds.

A worn part works worse: a worn engine gives less power, a broken one none; a flat tyre grips less. What it runs
into faster than two metres a second it pushes aside, and faster than four it runs over, harming it more the
faster it goes; the driver is credited with what it runs over.

A player's blows break it back into its item, carrying everything it is and holds: its parts as worn, its fuel,
its boot and its paint, and its keys still open it when it is placed again. In creative one blow takes it away.

### Trailers

A kind with a `hitch` is a trailer. Used with sneak held and an empty hand, it hitches to the nearest vehicle whose
`tow` point is within a block and a half of its hitch, and used so again it lets go. A hitched trailer follows
what tows it, swinging round behind it as a real one does, and lets go if it is pulled more than six blocks away.

### Its boot, its tanks and a fuel pump

Its boot and its tanks are reached through NeoForge's capabilities on the entity, so hoppers, pipes and machines
reach them: `Capabilities.ItemHandler.ENTITY` gives its boot, `Capabilities.FluidHandler.ENTITY` its fuel tank and
its cargo tanks, the fuel tank first, and `Capabilities.EnergyStorage.ENTITY` its battery, which takes charge and
gives none.

A **fuel pump** is the `FuelPump` a block entity holds: a tank of liquid, a tank a gas is pressed into and a store of
charge, filled by pipes, gas lines and cables, which fills every vehicle within three blocks every half second with
what that vehicle runs on.

```java
public static final class PumpBlockEntity extends SyncedBlockEntity {

    private final FuelPump pump = new FuelPump(new FuelPump.Size(16_000, 16_000, 10_000_000),   // mB, mB, FE
            new FuelPump.Flow(250, 5_000, 200_000), this::setChanged);                         // a half second

    public PumpBlockEntity(final BlockPos pos, final BlockState state) {
        super(MyContent.PUMP_ENTITY.get(), pos, state);
        fields().part("Pump", this.pump).save();
    }

    public static void tick(final Level level, final BlockPos pos, final BlockState state,
                            final PumpBlockEntity entity) {
        if (level instanceof ServerLevel server) {
            entity.pump.tick(server, pos);
        }
    }
}

// on the mod's bus
modBus.addListener(RegisterCapabilitiesEvent.class, event ->
        FuelPump.offer(event, MyContent.PUMP_ENTITY.get(), PumpBlockEntity::pump));
```

### Aboard moving structures, and through portals

A vehicle stands on a moving structure as anything does ([Moving structures](MOVING_STRUCTURES.md)): parked on a
ferry's deck, it goes where the ferry goes. A player driving on a deck in flight drives it as on the ground, their
game telling the server where on the deck it stands, so a deck as fast as a rocket never reads as the vehicle going
too fast. What it runs into and how hard it lands are read against the deck under it. Through a portal, it takes
its riders with it.

### Drawing it

A kind's model is drawn turned the way the vehicle faces and leaning as it leans, in its paint's texture. Bones of
the model named after its places are worked as the vehicle goes: a place's bone is hidden while its place is
empty; a wheel's bone turns as it rolls, turns with the steering when it steers, and drops on its spring to the
ground under it; a `<place>_spin` bone spins with a rotor or a propeller; a door's bone swings open; and a
`<place>_glow` bone shows only while that light shines.

## Robots

*Added 2026-10-05.*

A robot is a creature that works through a list of tasks, as its owner would: it never fights, and it never breaks
a block its owner could not break.

```java
public static final class Helper extends RobotEntity {
    public Helper(final EntityType<? extends Helper> type, final Level level) {
        super(type, level);
    }
}
// declared with .attributes(RobotEntity::robotAttributes)

robot.setOwner(player.getUUID());
robot.queue(new CoreRobotTasks.MoveTo(pos));
robot.queue(new CoreRobotTasks.BreakBlock(stone));
robot.queue(new CoreRobotTasks.Collect(4));      // picks up items within 4 blocks
robot.queue(new CoreRobotTasks.Wait(20));        // ticks
```

- The Core's tasks: `MoveTo`, `BreakBlock` (takes longer for harder blocks, and gives up after a minute), `Collect`
  (up to 16 blocks around), `Wait`, and `Drive` (drives the vehicle it sits in). A robot works on a block within 3
  blocks of it.
- A task answers each tick whether it is still running, done or failed. A failed task is dropped and the robot goes
  on to the next.
- Its tasks, its 9-slot inventory and its owner are saved; a task in progress starts over when the world loads. It
  drops its inventory when it dies, and never despawns.
- A mod adds a kind of task: implement `IRobotTask` and declare its type with a codec, once, from your mod's
  constructor: `RobotTasks.declare(new RobotTaskType<>(rl("plant"), PlantTask.CODEC))`.

## Projectiles

*Added 2026-10-05.*

```java
public static final ProjectileSpec BOLT_SPEC = new ProjectileSpec(
        4.0F,     // damage
        0.0,      // gravity: 0 flies straight; an arrow is 0.05
        1,        // how many things it passes through before it stops
        0.0,      // knockback
        0.0F,     // seconds it sets what it hits on fire
        0.0F,     // explosion at the end: 0 none, 4 is TNT's
        false,    // whether the explosion breaks blocks
        100);     // ticks it lives

public static final class Bolt extends CoreProjectile {
    public Bolt(final EntityType<? extends Bolt> type, final Level level) {
        super(type, level);
    }

    @Override
    public ProjectileSpec spec() {
        return BOLT_SPEC;
    }
}

Projectiles.fireFrom(player, MyContent.BOLT.get(), 2.0F);            // from the eyes, where they look
Projectiles.fire(level, owner, MyContent.BOLT.get(), from, direction, 2.0F);
```

Whoever fired it is credited with what it hits.

## What is worn

*Added 2026-10-05.*

```java
boolean suited = WornItems.wears(player, stack -> stack.is(MyContent.SPACE_HELMET.get()));
List<ItemStack> all = WornItems.worn(player);
```

`WornItems` looks at the armour slots and at every other place items are worn: the slots of **Curios** or of
**Accessories** when either is installed (Accessories carries Curios' slots too, so only it is asked when both
are). The stacks given are the worn ones themselves, to read, not to change. A mod with slots of its own adds them
with `WornItems.addSource(...)`.

## What can go wrong

- **The entity is invisible or the game crashes when it appears.** It has no renderer registered on the client.
- **The vehicle will not move.** Its ignition is off, it has no engine or a broken one, its fuel or battery is
  empty, or nobody who may drive it sits in a driving seat. A robot drives only with a `Drive` task.
- **The vehicle is invisible.** Its kind names no model, or a model the Core's models do not know.
- **The fuel tank takes nothing.** Its engine does not burn that fuel, or the fluid is not in the fuel's tag; a
  liquid and a gas never share the tank.
- **A trailer will not hitch.** No vehicle's `tow` point is within a block and a half of its `hitch`.
- **A player cannot get in.** It is locked, or every seat they may sit in is taken.
- **A robot stands still.** Its task failed (unreachable, too far, a block its owner may not break) and it has
  nothing after it, or it has no owner.
- **A worn item is not found.** It is worn in a slot mod other than Curios and Accessories, which needs a source of
  its own.
