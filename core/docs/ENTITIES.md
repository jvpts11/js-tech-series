# Entities

Things in the world that move: creatures, vehicles, robots, shots. J's Core declares them like blocks, and gives
three kinds ready to build on: vehicles a player drives, robots that work a list of tasks, and projectiles. It
also finds what a player or a creature is wearing, wherever it is worn.

## Declaring an entity

*Added 2026-10-05.*

```java
public static final EntityEntry<Rover> ROVER = CONTENT.entity("rover", Rover::new, MobCategory.MISC)
        .named("Rover")
        .size(1.4F, 0.9F)          // width and height in blocks
        .holdsEnergy()             // chargers and cables reach its energy
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

*Added 2026-10-05.*

A vehicle is an entity players sit in and drive. Say what it is like in a `VehicleSpec`, and extend
`VehicleEntity`:

```java
public static final VehicleSpec ROVER_SPEC = new VehicleSpec(
        List.of(new Vec3(0.0, 0.4, 0.3), new Vec3(0.0, 0.4, -0.5)),   // seats; the first is the driver's
        0.5,      // top speed, blocks a tick
        0.08,     // how fast it gets there
        4.0F,     // how many degrees it turns a tick
        0.42,     // how fast it climbs
        false,    // whether it flies
        10_000,   // its battery, in FE
        5);       // what it spends a tick while moving, in FE

public static final class Rover extends VehicleEntity {

    public Rover(final EntityType<? extends Rover> type, final Level level) {
        super(type, level);
    }

    @Override
    public VehicleSpec spec() {
        return ROVER_SPEC;
    }

    @Override
    protected ItemStack asItem() {
        return new ItemStack(MyContent.ROVER_ITEM.get());   // what it breaks back into
    }
}
```

The player drives with the walking keys. A flying vehicle climbs with jump and sinks with sprint (sneak is how a
player gets off). A vehicle that does not fly falls by the gravity of its dimension ([World](WORLD.md)). Its
battery is saved and shown to players; with no energy it does not move. A player's blow breaks it back into its
item.

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
- **The vehicle will not move.** Its battery is empty, or nobody sits in the driver's seat (the first).
- **A robot stands still.** Its task failed (unreachable, too far, a block its owner may not break) and it has
  nothing after it, or it has no owner.
- **A worn item is not found.** It is worn in a slot mod other than Curios and Accessories, which needs a source of
  its own.
