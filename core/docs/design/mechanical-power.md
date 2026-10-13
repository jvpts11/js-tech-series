# Mechanical power

Rotation carried by shafts, gears, belts and chains, from water wheels, windmills, steam engines and motors to the
machines that turn with it. It is in the spirit of Create's kinetic system, with real physics, as everything in the
series.

## What exists today

The cable block knows a motion grid, with no kit behind it yet ([Cables and lines](cables-and-lines.md)).

## To build

### Real units

- **Speed in revolutions a minute, torque in newton-metres, power in watts**: power is torque times angular speed.
  Rotation and J's Energy turn into each other through **electric motors and dynamos** ([Energy](energy.md)). The kit is
  the platform's; the blocks are the mods'.
- **Gears trade speed for torque**, and the power stays, less the **friction** of each part: bearings and gears each
  lose a share.
- **Too little power slows things down instead of stopping them dead**: when the machines ask for more torque than the
  sources give, the whole shaft turns slower, sharing the power, as with too little energy; a machine below its lowest
  speed stops.

### The parts

- **Shafts and gears are blocks of their own**, turning in sight, with large gears and belts that span distances; the
  cable block's motion grid only tells what is joined to what.
- **Shafts on the three axes, gears** (direction and ratio), **belts and chains** (across a distance, with slip),
  **gearboxes, clutches** (by redstone), **brakes** and **speed controllers**.
- **Inertia and flywheels**: what turns keeps energy. A flywheel smooths a network and keeps turning after the power is
  cut; a heavy one takes time to start, as the flywheel that starts J's Industrial's tokamak.
- **Shear pins**: a spike of torque breaks the pin and stops the line without harming anything, as a fuse does. Parts
  breaking under overload is a consequence setting.
- **Sources declare their curves** of torque and speed: water wheels, windmills (with the weather kit's wind and the
  altitude), steam engines, motors, hand cranks.

### Seeing and measuring

- **Parts turn at their real speed**, drawn by instancing; belts run; the sound follows the speed.
- **Tachometers and torque meters** as readings for sensors; Jade shows speed, torque and power; the network probe works
  on shafts.

### Together with the rest

- **Mechanical power on moving structures**: an airship's propellers driven by shafts, as in Create Aeronautics
  ([Moving structures](moving-structures.md)).
- **Networks are worked out again only when they change**, with no tick per shaft.
