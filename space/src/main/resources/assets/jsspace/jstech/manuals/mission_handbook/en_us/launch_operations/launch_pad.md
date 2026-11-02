---
section: launch_operations
order: 8
icon: jsspace:flame_deflector
covers: [jsspace:flame_deflector, jsspace:pad_service_port, jsspace:trench_lining]
---
# Launch Pad

## What it is

The ground a rocket leaves from, where the [crawler](jsspace:crawler) sets its launcher down. Under the launcher's opening lies the flame trench: a row of Flame Deflectors in the ground between two walls of Trench Lining. By the pad stands the Pad Service Port, where the pad's lines come up.

## What it is for

Sparing the pad and fuelling the rocket. The deflectors split the exhaust both ways along the trench, and at ignition the service port pours its deluge of water over the pad. The port also fuels the rocket on the launcher over it, tank by tank, its pumps spending energy for every kilogram.

## How to get it

From the creative menu, J's Space tab.

## How to use it

```multiblock
id: jsspace:launch_pad
caption: The Launch Pad, built, its trench dug into the ground
height: 120
```

1. Build the pad from a Structure Projector's ghost, or use a Multiblock Builder on the projector: a square of light gray concrete with its corners cut; the flame trench dug five blocks deep along its middle, lined with Trench Lining; a band of Flame Deflectors across the trench's floor under the pad's middle; the crawler's two lanes of gravel, from the pad's edge to three blocks past its middle; and the Pad Service Port by the pad.
2. Run the crawlerway from the hangar's door straight to the pad's lanes. The crawler stops where they end, and sets the launcher down with its opening over the deflectors, whatever rocket stands on it. You may put a column of three Pad Pedestals under each of the launcher's six there, as in its hangar.
3. Pipe water into the service port, and liquid oxygen and liquid methane from the [propellant depot](jsspace:propellant_depot), and give it energy.

A pad built by hand works the same way: its deflectors across the trench, facing along it, at most eight blocks below the ground with only air above them up to the opening, the row's ends closed with Trench Lining; and its service port on the ground within sixteen blocks of the rocket's axis.

Table: What the Pad Service Port holds

| Line | Holds |
| --- | --- |
| Water, a whole deluge | 64,000 mB |
| Liquid oxygen | 32,000 mB |
| Liquid methane | 32,000 mB |
| Pump energy, a whole deluge | 400,000 J |

## What can go wrong

> [!WARNING]
> No flame trench and no deluge: the launch wrecks the pad and what is around it, unless the server turns launch damage off.

> [!PROBLEM] The launch panel's DELUGE reads NO-GO.
> The service port holds less than a whole deluge. Top up its water and its energy.

> [!PROBLEM] The rocket does not fill.
> The port finds no launcher within sixteen blocks round it and twelve above, its lines are empty, or its pumps have no energy. Nothing ever comes back out of its lines.
