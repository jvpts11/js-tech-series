---
section: launch_operations
order: 7
icon: jsspace:crawler_engine
covers: [jsspace:crawler_cab, jsspace:crawler_deck, jsspace:crawler_engine, jsspace:crawler_track, jsspace:leveling_jack]
---
# Crawler-Transporter

## What it is

A slow tracked vehicle that carries a [mobile launcher](jsspace:launcher) and its rocket from the hangar to the pad. Its hull is fifteen blocks long and eleven wide: the Crawler Engine in the middle, Crawler Deck round it. Under each corner, a truck four blocks long and two wide has Crawler Tracks on the ground and Leveling Jacks over them. A Crawler Cab, two blocks square and two tall, stands at two opposite corners, outside the hull's ends.

## What it is for

The rollout. It lifts a launcher centred over it off its pedestals, drives to the far end of its crawlerway at 0.45 blocks a second, sets the launcher down and comes back at 0.9. Empty, it drives to the other end and stays. Bay Doors in its way open for it, and close when its trip is over.

## How to get it

From the creative menu, J's Space tab.

## How to use it

```multiblock
id: jsspace:crawler_transporter
caption: The Crawler-Transporter, built
height: 100
```

1. Lay the crawlerway: a lane of gravel under each side's tracks, up to 1,024 blocks, ending where the crawler is to stop.
2. Build the crawler along it, from a Structure Projector's ghost, or use a Multiblock Builder on the projector. Its engine finds it whole and forms it.
3. For a rollout, stand it under the launcher's deck, centred, its length along the deck's.
4. Use a cab. The hangar hands the rocket's plan to its [flight computer](jsspace:flight_computer), and the crawler sets off.

## What can go wrong

> [!PROBLEM] The cab says the crawler is not whole.
> A track, jack, deck plate or cab is missing round the engine.

> [!PROBLEM] The cab says there is no crawlerway.
> Lay gravel under every track, along the crawler's length.

> [!PROBLEM] The cab says the way is blocked at a place.
> Something stands in the path of the crawler, the launcher or the rocket; only Bay Doors open for it.

> [!PROBLEM] The cab says the rocket is not finished, or has no flight computer.
> Finish the rocket in the hangar, with its Flight Computer.
