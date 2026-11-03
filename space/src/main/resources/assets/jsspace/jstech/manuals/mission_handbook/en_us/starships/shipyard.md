---
section: starships
order: 1
icon: jsspace:shipyard_terminal
covers: [jsspace:shipyard_terminal, jsspace:booth_panel, jsspace:cradle_clamp, jsspace:dock_feeder, jsspace:dock_floodlight, jsspace:dock_gantry, jsspace:dock_girder, jsspace:dock_rail, jsspace:dock_stairs, jsspace:dock_tower, jsspace:dock_walkway, jsspace:pit_rim, jsspace:item_port]
---
# Small Shipyard

## What it is

The dry dock a small starship is built in: a pit sixteen blocks wide, twenty-four long and three deep, ringed by Pit Rim, with Cradle Clamps rising from its floor. Lattice Towers, Dock Girders, Dock Walkways and Gantry Rails line its sides, under the Gantry Bridge and four Dock Floodlights. Dock Stairs climb one side. At the back of the other, a booth of Control Booth Panels holds the Shipyard Terminal, over an Item Port. A Block Feeder stands on each lower walkway.

## What it is for

Building a ship from a design, letting it go to fly, and taking it back to change or mend it.

## How to get it

From the creative menu, J's Space tab. The Item Port is in the same tab.

## How to use it

```multiblock
id: jsspace:small_shipyard
caption: The Small Shipyard, built
height: 120
```

1. Build the dock whole, its pit dug clear. J's Core's Structure Projector shows it block by block, and the Multiblock Builder used on that projector digs the pit and builds the dock for you, its terminal up in the booth last.
2. Use the terminal to open the Starship Design. Drag parts from their cards to the green node of a joint; the right mouse button turns the view.
3. Mind the flight check: a cockpit, an airlock, a reactor, a main engine, landing gear, a whole spine, and a ship that fits the bay.
4. Press [[S]] to keep the design, [[E]] to send it to the dock.
5. Build it by hand on the ghosts of its hologram, or feed the Item Port by hopper or pipe for the four robot arms; the Starship Design shows what is still to bring. Each part forms into its model as its last block goes in.
6. With every part formed, use the Flight Console with an empty hand. The ship leaves the dock, yours to [fly](jsspace:flying_a_ship).
7. To change or mend it, bring it within 32 blocks of a free dock's terminal and, as its pilot, use its Flight Console. The dock takes it back, fills its reactor and keeps its plan, to change and send again with [[E]].

On a J's Computers network, wired by a data cable beside the terminal, RUN jscore:assemble WITH plan = 'NAME' sets the arms building a kept plan.

## What can go wrong

> [!PROBLEM] The terminal opens a plain machine screen.
> The dock is not whole: the screen says what it lacks. The pit must be empty.

> [!PROBLEM] The Flight Console does not start the ship.
> A part is not formed yet: its tag in the hologram counts its blocks in place.

> [!CAUTION]
> Breaking the terminal takes every formed part apart, and the arms drop the blocks they hold.
