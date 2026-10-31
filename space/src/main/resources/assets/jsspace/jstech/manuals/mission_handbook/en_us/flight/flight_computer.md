---
section: flight
order: 1
icon: jsspace:flight_computer
covers: [jsspace:flight_computer]
---
# Flight Computer

## What it is

The computer built into every [capsule](jsspace:capsule) and into the cabin of every [lander](jsspace:lander): a flat display with keys along its top and down each side. It plans a flight, then flies it.

## What it is for

Getting from one world to another: the transfer, the landing, the way back, and where each spent stage goes.

## How to get it

From the creative menu, J's Space tab.

## How to use it

Use it with an empty hand to open its screen, then click a key or the words beside it. The keys along the top open its pages; SYS and COMM open nothing yet.

Table: Its pages

| Page | Its keys |
| --- | --- |
| XFER | Left: the world to go to; WIN - and WIN +, the window to leave in. Right: DV spends more delta-v for a shorter trip and TOF less; ASSIST swings past a world, spending less and taking longer; AERO brakes in the air on arrival instead of burning; EXEC sets the plan. |
| LAND | Click the map to mark a spot. Left: ZOOM +, ZOOM -, MARK, CLEAR. Right: HAZARD shows ground too steep to land on, GRID, and SET, which sets the landing at the mark. GUIDE is lit while a guidance platform guides the rocket. |
| STG | Left: the stage. Right: ANY, ZONE, PLAT, PLAT + and RAD +, where it goes once spent. |

1. On the ground, set where each spent stage goes on STG, the transfer on XFER with EXEC and the landing on LAND with SET.
2. Launch from the [launch panel](jsspace:launch_panel). The computer lets each burnt-out stage go, and past the top of the sky the rocket comes to rest in the world's low orbit.
3. When the window comes, it burns and sets out; between the Overworld and the Moon there is no window. If everyone aboard sleeps, the cruise passes at once.
4. In the other world's orbit, it [comes down](jsspace:coming_down).
5. Landed, set a new plan with EXEC: it lifts off when its window comes.

> [!SEE] jsspace:landing_platform, jsspace:rocket_wreckage

## What can go wrong

> [!CAUTION]
> Choosing another world on XFER clears the plan, the mark and the landing.

> [!PROBLEM] XFER reads NO TRANSFER FROM HERE.
> It plans only on a world's ground. Set the plan before lift-off.

> [!PROBLEM] XFER reads NOT ENOUGH DV.
> The stages left cannot pay for the burn and the capture, so the rocket will not set out. Spend less with TOF, turn on AERO or ASSIST, or wait for a window rather than leaving now.

> [!PROBLEM] STG reads NO ROCKET PLAN.
> The computer takes its rocket's plan when the rocket leaves its hangar.
