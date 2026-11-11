---
section: library_reach
order: 468
---
# Navigation: flying the structure your computer stands on

A computer built onto a moving structure of J's Core, a ship, an airship or a cart, can fly it. Your program tells the structure's autopilot where to go, and reads what a flight sensor would read where the computer stands. You do both through Navigation, a built-in type of System.Navigation. Navigation came with Sigma# 3.

Table: Navigation readings

| Member | What it gives |
| --- | --- |
| `Navigation.Aboard` | Yes when the computer stands on a moving structure. |
| `Navigation.Piloted` | Yes while an autopilot flies it. |
| `Navigation.X, Navigation.Z` | Where the computer is in the world, east and south, in metres. |
| `Navigation.Altitude` | How high it is, in metres. |
| `Navigation.Pressure` | The air pressure there, in kilopascals. |
| `Navigation.Speed` | How fast it goes, in metres a second. |
| `Navigation.Climb` | How fast it goes up, in metres a second; below nothing when it goes down. |
| `Navigation.Pitch, Navigation.Roll` | How far its nose is up and how far it leans to the side, in degrees. |
| `Navigation.Heading` | Where it looks, in degrees from north round by east. |
| `Navigation.AboveGround, Navigation.ClearAhead` | How far down the ground is, and how far ahead the first solid block is, in metres, up to 64 metres. |

Table: Navigation commands

| Member | What it does |
| --- | --- |
| `Navigation.GoTo(x, y, z, speed)` | Flies to that place of the world and holds there. |
| `Navigation.HoldAltitude(y, speed)` | Holds that height, and leaves the other controls to the helm. |
| `Navigation.FollowRoute(points, speed, loop)` | Flies to each point in turn: round again with loop yes, else holding at the last. |
| `Navigation.Dock(x, y, z, speed)` | Docks to the clamp of another structure at that place of the world. |
| `Navigation.Release()` | Takes the autopilot off. |

Each command gives yes when the computer stands on a structure to fly, and no on the ground, where the readings are those of the place the computer stands, still. A speed is the fastest the autopilot goes, from 0.01 to 100000 metres a second; any other stops your program with an ArgumentOutOfRangeException. Each reading costs a glance, and each command costs what a write does.

A route is a List<double> of points, each written as its x, y and z, with up to 256 points. Dock gives no when no other structure has a clamp at that place; the autopilot comes in slowly from off the clamp's face and lets go once the clamps hold. A pilot at the helm still steers whatever the autopilot does.

```sgs Patrol.sgs
if (Navigation.Aboard && !Navigation.Piloted) {
    Navigation.HoldAltitude(120, 4);
}
```

> [!DEFINE] Autopilot
> The part of a moving structure that works its engines towards a place, a height or a clamp, so that you need not hold the helm.

## If something goes wrong

> [!PROBLEM] Every command gives no.
> The computer is not on a moving structure. Build it onto one, and check Navigation.Aboard.

> [!PROBLEM] The structure does not move.
> It needs engines that push it the way it is told to go.

> [!SEE] jsc:sensors_library, jsc:signals_library
