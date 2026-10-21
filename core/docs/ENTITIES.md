# Entities

Things in the world that move: creatures, vehicles, robots, shots. J's Core declares them like blocks, and gives
four kinds ready to build on: vehicles a player drives, robots that work a list of tasks, creatures that live as
their data says, and projectiles. It also finds what a player or a creature is wearing, wherever it is worn, and runs
the modules in worn things.

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

*Added 2026-10-05; flying, swimming, batteries, tools and tasks with logic 2026-10-21.*

A robot is a creature that works through a list of tasks, as its owner would: it never fights, and it never breaks
a block its owner could not break.

```java
public static final class Helper extends RobotEntity {
    public Helper(final EntityType<? extends Helper> type, final Level level) {
        super(type, level);
    }

    @Override
    public RobotMovement movement() {
        return RobotMovement.FLY;           // WALK, the default, FLY or SWIM
    }

    @Override
    public int batteryCapacity() {
        return 50_000;                      // joules; 0, the default, runs on nothing
    }
}
// declared with .attributes(RobotEntity::robotAttributes), and .holdsEnergy() for one with a battery

robot.giveTo(player.getUUID());
robot.queue(new CoreRobotTasks.MoveTo(pos));
robot.queue(new CoreRobotTasks.BreakBlock(stone));
robot.queue(new CoreRobotTasks.Collect(4));      // picks up items within 4 blocks
robot.queue(new CoreRobotTasks.Wait(20));        // ticks
```

- **How it gets about.** A walker walks; a flyer flies in three dimensions and never falls; a swimmer swims in three
  dimensions in water and walks on land between waters. No robot drowns.
- **Its inventory** is as big as the robot: a slot for every twentieth of a cubic metre of its box, from 1 to 54. A
  robot as wide as a player and two thirds as tall carries nine. A robot may say otherwise (`inventorySize()`).
- **Tools.** It digs with the tool it carries that digs the block fastest, of any mod, taking it into its hand, at
  the pace a player digs with it; the tool wears and its enchantments work on what drops. Without one it digs with its
  own arm, as fast as a wooden pickaxe, and still gets what the block drops.
- **A battery**, when it has one: getting about spends it (walking 400 W, swimming 600 W, flying 1.2 kW) and so does
  digging (800 W). With an empty battery it stands still until it is charged, through the game's energy capability
  or at a **charging station**: a `RobotCharger` a block entity holds, as a fuel pump holds its `FuelPump` (see
  Vehicles), which charges every robot within two blocks every half second.

The Core's tasks:

| Task | What it does |
| --- | --- |
| `MoveTo(pos)` | Goes beside a block; gives up after a minute. |
| `BreakBlock(pos)` | Goes to a block and breaks it, as above; gives up after a minute. |
| `Collect(radius)` | Picks up the items within up to 16 blocks. |
| `Wait(ticks)` | Waits. |
| `Drive(input, ticks)` | Drives the vehicle it sits in. |
| `GoToNearest(block, radius)` | Goes to the nearest block of a kind, as it stands when the task begins. |
| `BreakNearest(block, radius)` | Breaks the nearest block of a kind. |
| `Recharge(radius)` | Goes to the nearest working charging station and waits until its battery is full. |
| `Repeat(tasks, times)` | Works through its tasks so many times, or for ever with 0. |
| `If(condition, then, else)` | Works through one list or the other, as the condition holds when it begins. |
| `WaitUntil(condition, timeout)` | Waits until the condition holds; gives up after the timeout, or never with 0. |
| `AtTime(time)` | Waits for the day's clock to come to a time: 0 at sunrise, 6000 at noon. Repeated, it is a schedule. |

A task that repeats or chooses works through fresh copies of its tasks, one at a time, and one step that fails ends it,
failed, so a loop never spins on what cannot be done.

Its conditions, what it senses of itself and the world:

| Condition | Holds when |
| --- | --- |
| `Holding(item, count)` | It carries at least so many of an item. |
| `InventoryFull()` | Its inventory has no empty slot. |
| `BlockIs(pos, block)` | A place holds a block of a kind. |
| `BlockNear(block, radius)` | A block of a kind is within so many blocks. |
| `ItemsNear(item, radius)` | Items lie within so many blocks, of a kind or any. |
| `EntityNear(entity, radius)` | A creature or thing of a kind is within so many blocks. |
| `TimeBetween(from, to)` | The time of day is in a span; one past midnight wraps round. |
| `ChargeBelow(share)` | Its battery is less full than a share. |
| `Not`, `All`, `Any` | One turned round, every one, at least one. |

Blocks, items and creatures are named as the game's advancements name them: `{"blocks": "minecraft:stone"}`,
`{"items": "#minecraft:logs"}`, `"minecraft:pig"`. So a robot gathering wood until its inventory is full is:

```java
robot.queue(new CoreRobotTasks.Repeat(List.of(
        new CoreRobotTasks.If(new CoreRobotConditions.InventoryFull(), List.of(), List.of(
                new CoreRobotTasks.BreakNearest(BlockPredicate.Builder.block().of(BlockTags.LOGS).build(), 16),
                new CoreRobotTasks.Collect(4))),
        new CoreRobotTasks.WaitUntil(new CoreRobotConditions.Not(new CoreRobotConditions.InventoryFull()), 0)), 0));
```

- A task answers each tick whether it is still running, done or failed. A failed task is dropped and the robot goes
  on to the next.
- Its tasks, its inventory, its charge and its owner are saved; a task in progress starts over when the world loads.
  It drops its inventory when it dies, and never despawns.
- A mod adds a kind of task: implement `IRobotTask` and declare its type with a codec, once, from your mod's
  constructor: `RobotTasks.declare(new RobotTaskType<>(rl("plant"), PlantTask.CODEC))`; and a kind of condition the
  same way, through `RobotConditions.declare`.

### Commanded from outside

What commands robots from outside, a computer's program, reaches them through `Robots`:

```java
Robots.find(id);                                  // the loaded robot of an id
Robots.ownedBy(player);                           // every loaded robot a player owns
Robots.status(robot);                             // where it is, its charge, its tasks, its slots
Robots.read(RegistryOps.create(JsonOps.INSTANCE, registries), program)   // a program written as data
        .result().ifPresent(tasks -> Robots.program(robot, tasks));      // its tasks replaced by these
JsCore.events().subscribe(RobotTaskEvent.class, event -> ...);          // each task as it ends, done or failed
```

A program written as data is a list of tasks, each with its type: `[{"type": "jscore:move_to", "target": [0, 64, 0]},
{"type": "jscore:break_nearest", "block": {"blocks": "minecraft:stone"}, "radius": 8}]`.

## Creatures

*Added 2026-10-21.*

A creature is a `CreatureEntity` a mod declares, which lives as its data says: how much health it has, how fast it
goes, how hard it hits, what it does, where it appears and the air it breathes. A creature appears only where its air
suits it, and lives only there: out of it, it chokes, a point of harm a second.

```java
public static final EntityEntry<CreatureEntity> GRAZER = CONTENT.entity("grazer", CreatureEntity::new,
        MobCategory.CREATURE).size(0.9F, 1.2F).attributes(CreatureEntity::creatureAttributes).named("Grazer")
        .register();

Creatures.declare(GRAZER.getId(), CreatureDef.builder()
        .health(14).speed(0.3)
        .behaviour(new CoreCreatureBehaviours.KeepAfloat())        // the first matters most
        .behaviour(new CoreCreatureBehaviours.Panic(1.25))
        .behaviour(new CoreCreatureBehaviours.Wander(1.0))
        .spawn(new CreatureDef.Spawn(List.of(), List.of(Level.OVERWORLD), 10, 1, 3, Optional.empty(),
                CreatureDef.Placement.ON_GROUND))
        .breathes(AirMix.OXYGEN, 16, 50)                           // kilopascals of the gas it needs, and bears
        .build());
```

A data pack writes or changes it under `data/<namespace>/jstech/creatures/`, by its entity type's id:

```json
{
  "health": 14, "speed": 0.3,
  "behaviours": [{"type": "jscore:float"}, {"type": "jscore:panic"}, {"type": "jscore:wander"},
                 {"type": "jscore:tempt", "item": {"items": "minecraft:wheat"}}],
  "spawns": [{"biomes": ["#minecraft:is_forest"], "dimensions": ["minecraft:overworld"], "weight": 10,
              "least": 1, "most": 3}],
  "air": {"gas": "jscore:oxygen", "least": 16, "most": 50}
}
```

- **Behaviours**: `wander`, `look_at_players`, `look_around`, `float`, `panic`, `avoid` (keeps away from a kind of
  creature), `tempt` (follows a player holding its food), `melee_attack`, `target_players` and `retaliate`. A mod adds
  its own kinds through `CreatureBehaviours.declare`. What it does is built from its data as it first wakes, and again
  when the server reads its data packs again.
- **Where it appears**: in biomes by id or tag, of the dimensions named (none for any), placed on the ground, in water,
  in lava or anywhere, in groups. The Core adds every creature's spawns to their biomes as the server starts, through
  its biome modifier `jscore:creatures`; a data pack read again with the server running changes how creatures live at
  once, and where they appear when the server starts again.
- **The air it breathes**: a gas, by the air kit's id, and the pressure of it in the air it lives on and bears, in
  kilopascals, as a sealed room's air or the dimension's has it ([Rooms](ROOMS.md)). People need about 16
  kilopascals of oxygen and bear about 50. A creature with no air named breathes nothing.

## Projectiles

*Added 2026-10-05; real ballistics 2026-10-21.*

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

Whoever fired it is credited with what it hits. It falls by the pull of the dimension it flies in: its gravity is its
fall under the overworld's pull, a sixth as much on a moon with a sixth of it.

A projectile with a mass flies by **real ballistics**: the air holds it back by its drag area and by the air's density
where it flies, from a sealed room's air or the dimension's at that height, thin up high and none in a vacuum; water
holds it far more; and the wind carries it. One that **gets through blocks** passes any block as hard as it can still
get through, slowed by it, and stops in the first it cannot:

```java
public static final ProjectileSpec SLUG = new ProjectileSpec(6.0F, 0.03, 0, 0.0, 0.0F, 0.0F, false, 200)
        .withBallistics(0.01, 0.0001)      // ten grams, a drag area of a square centimetre
        .withPenetration(2.5F);            // the hardness it gets through in all: glass 0.3, planks 2, stone 1.5
```

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

### Modules in worn things

*Added 2026-10-21.*

A worn item with sockets, a **host**, takes **modules**, each into a socket of its kind; each module on does what it
does for its wearer each second, spending the energy the host holds, and does nothing out of energy until the host is
charged. J's Industrial's exo-suits and J's Space's suits are hosts of this kit. Hosts and modules are declared by
their items' ids, in code or in data packs under `data/<namespace>/jstech/module_hosts/` and
`data/<namespace>/jstech/modules/`:

```java
Modules.declareHost(HELMET.getId(), ModuleHostDef.worn(List.of(
        new ModuleHostDef.Socket("visor", 1), new ModuleHostDef.Socket("core", 2))));
Modules.declare(SPEED_MODULE.getId(), new ModuleDef("core", 100, List.of(          // 100 FE a second while on
        new CoreModuleEffects.AttributeChange(Attributes.MOVEMENT_SPEED, 0.2,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE))));
```

```json
{"kind": "visor", "energy": 50, "effects": [{"type": "jscore:mob_effect", "effect": "minecraft:night_vision"}]}
```

- **Effects**: `attribute` changes one of the wearer's attributes (speed, armour, reach, how high it steps), and
  `mob_effect` gives one of the game's effects, quietly, for as long as the module works. A mod adds its own kinds
  through `ModuleEffects.declare`; an effect is asked each second, and what it stops asking for goes, so a module
  taken out, turned off or out of energy leaves nothing behind.
- **The modules in a host** are its `jscore:modules` component, socket by socket, each on or off.
  `Modules.install(level, host, socket, module)` puts one into an empty socket of its kind, `Modules.remove` takes it
  out, and `Modules.setOn` turns it on or off.
- The host's energy is its own, as an item holds it ([Content](CONTENT.md)); it must give each second what its modules
  spend, at once. A host is worn anywhere things are worn: its armour slot, or a slot of Curios or Accessories.
- A tool can be a host too, whose modules work while it is held and spend each use ([Tools and
  items](TOOLS.md#modules-in-tools)).

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
  nothing after it, its battery is empty, or it has no owner.
- **A robot breaks a block it was not meant to.** `BreakNearest` takes the nearest block of its kind within its
  radius, the ground under it too: name the kind closely, or keep the radius small.
- **A creature never appears.** No spawn of its data names the biome, the dimension or the placement where it would,
  or the air there does not suit it; spawns read again by a data pack wait for the server to start again.
- **A module does nothing.** It is off, in a socket of another kind, or its host is out of energy, or gives less at
  once than the module spends in a second.
- **A worn item is not found.** It is worn in a slot mod other than Curios and Accessories, which needs a source of
  its own.
