# Conveyors

Items carried on surfaces: belts, chutes, lifts, with machines that work them as they pass. The kit is the platform's;
J's Industrial's belts are one use of it. It is in the spirit of Create's belts, funnels and chutes and Immersive
Engineering's conveyors.

## What exists today

Everything this page designs is built, as of October 2026. How a mod uses it, with code:
[Conveyors](../CONVEYORS.md).

## The design

- **Items in compressed queues, never entities**, so thousands of moving items weigh nothing. Measured: 3,680 items
  riding 2,024 belts at once cost the server about a millisecond a tick, a third of a microsecond an item. The end of
  a run keeps the run it walked, walking its pieces again only once a piece is laid, broken or turned, so a long run
  costs its items and not a search of its pieces every tick.
- **Items seen moving**, at a speed by the kind of belt.
- **Splitting, merging and sorting by filter**: splitters, joins, diverters with filters.
- **Up and down**: chutes, bucket elevators, sloped belts.
- **Work done on the belt**: machines that work an item as it passes (pressing, washing, painting), as Create's do.
- **Players and mobs are carried** by a belt.
- **Driven by energy or by mechanical power**, through either kit ([Energy](energy.md),
  [Mechanical power](mechanical-power.md)).
- **They work on moving structures** ([Moving structures](moving-structures.md)).
- **A pneumatic post, for items sent far**: stations with an address on a line of tubes of their mod's, whose runs
  join the stations on them. A station sends what it is given to the address its routes give each item, by filter,
  pushing only; a capsule carries a few stacks and takes time by the distance, kept with the world on its way, and
  unloads into the station it reaches. Its mod says how much a capsule carries, how often a station sends and how
  fast a capsule goes, and what a station spends to send.
