---
section: launch_operations
order: 10
icon: jsspace:launch_panel
covers: [jsspace:launch_panel]
---
# Launch Panel

## What it is

The LCC-1 console by the pad, where a launch is counted down and given its go. It finds the launcher on its pad within 24 blocks round it, 8 below and 16 above, and reads its rocket. Its board shows the go or no-go of each system: vehicle, propellant, tower arms, deluge, weather, crew, range and clamps.

## What it is for

Launching. The count runs from T-60: the tower arms swing back from T-42, the LAUNCH button's cover lifts at T-10, the engines light at T-6 and the rocket goes at T-0. The count runs on the rocket's [flight computer](jsspace:flight_computer) too, and whoever presses LAUNCH owns the rocket in flight, for their team to use.

The count is the [launcher](jsspace:launcher)'s, one for its pad, not the panel's. The panel and the launch page of the rocket's flight computer both show it and work it: what is done on one shows on the other, and either holds it. A pad needs no panel; a crew can count down from the capsule alone.

## How to get it

From the creative menu, J's Space tab.

## How to use it

Use the panel to open its console; work its controls with the mouse.

- [ ] VEHICLE ... GO
- [ ] PROPELLANT ... GO, LOX and CH4 at 99%
- [ ] WEATHER ... GO
- [ ] DELUGE ... AUTO
- [ ] CLAMPS ... AUTO
- [ ] MASTER key ... ARM
- [ ] IGNITION guard ... lift
- [ ] IGNITION ... ARM
- [ ] COUNT RUNNING ... press
- [ ] LAUNCH ... press from T-10, before T-6

```card
title: Leaving a world 1/4
[ ] Rollout: launcher on the pad
[ ] Fuelling: LOX and CH4 at 99%
[ ] Board: all GO
```

```card
title: Leaving a world 2/4
[ ] Arming: MASTER, guard, IGNITION
[ ] T-60: COUNT RUNNING
[ ] T-42: tower arms swing back
```

```card
title: Leaving a world 3/4
[ ] T-10: cover lifts, press LAUNCH
[ ] T-6: engines lit, deluge poured
[ ] T-0: release, the climb begins
```

```card
title: Leaving a world 4/4
[ ] Staging: spent stages let go
[ ] Space: the rocket passes into space
```

## What can go wrong

> [!PROBLEM] COUNT RUNNING does nothing.
> MASTER or IGNITION is not armed, or the vehicle, the propellant, the weather or the clamps is no-go.

> [!PROBLEM] The count holds at T-6.
> LAUNCH was not pressed by ignition. Press it: the count runs on while all is go.

> [!PROBLEM] The count holds by itself.
> A system went no-go, a thunderstorm came over the pad, or MASTER was turned to SAFE. Press COUNT RUNNING once all is go again. RECYCLE T-10 takes a hold under T-10 back to T-10; SCRUB starts again from T-60 and turns MASTER to SAFE.

> [!PROBLEM] The launch is scrubbed at T-6, its engines unlit.
> The rocket weighs more on this world than its first stage pushes, and the server keeps such a rocket's engines off, so nothing is spent. Fit a larger engine, or make the rocket lighter. Where the server lets them light, a rocket too heavy stays on the pad burning its propellant.

> [!PROBLEM] ABORT lights at T-0.
> The rocket could not be released, and the launch is scrubbed.
