---
section: racks
order: 60
icon: jsc:cooling_tower_fan
covers: [jsc:cooling_tower_basin, jsc:cooling_tower_louvre, jsc:cooling_tower_fill, jsc:cooling_tower_fan, jsc:cooling_tower_terminal, jsc:chiller_evaporator, jsc:chiller_condenser, jsc:chiller_compressor, jsc:chiller_terminal, jsc:cdu]
---
# Liquid cooling

A Supercomputer Rack that runs hotter than its air can take away is cooled with water. Two liquids go round, through pipes: **Cold Water**, out to what it cools, and **Hot Water**, coming back. One millibucket a second carries one kilowatt.

> [!DEFINE] Loop
> A ring of pipe that the same water goes round: out cold, back hot, cooled again.

A rack takes away 1,000 W by air, plus its Cooling Units and Vent Panels. With cold water reaching it, it takes away up to **8,000 W more**, so a full rack of Supercomputer Nodes runs at full speed.

1. Build a **Chiller** and a **Cooling Tower** from the chain's kit and their own parts, as each machine's page in a recipe viewer shows, and place a **CDU** beside the racks. Feed each with energy: the Chiller and the tower through their Power Entries, the CDU at the bottom of its back left.
2. Pipe the **racks' loop**: the CDU's COLD outlet, in its roof at the front left as you face its front, to the inlet on top of each rack (the top block at its back right), and each rack's outlet (the top block at its front left) back to the CDU's HOT inlet, at the back left of its roof. One CDU feeds up to eight racks.
3. Pipe the **CDU's loop**: the Chiller's COLD outlet on its water box's end to the CDU's COLD inlet, at the front right of its roof, and the CDU's HOT outlet, at the back right, back to the Chiller's HOT inlet.
4. Pipe the **tower's loop**: the Chiller's HOT outlet on the back of its water box to the tower's HOT inlet, on its left; the tower's COLD outlet, on its right, back to the Chiller's COLD inlet on the front of its water box.
5. Pipe plain **water** into the tower's FILL inlet: it evaporates a millibucket for every ten it cools.
6. To fill the loops at first, pipe plain water into any COLD inlet: it is taken as Cold Water.

Table: What each machine does at full load

| Machine | Cools | Uses |
| --- | --- | --- |
| Supercomputer Rack | up to 8 mB/s, +8,000 W to its budget | cold water |
| CDU | up to 64 mB/s, eight racks | up to 1.5 kW, and 64 mB/s of the Chiller's cold water |
| Chiller | up to 120 mB/s, 120 kW | 30 kW, and 150 mB/s of the tower's cold water |
| Cooling Tower | up to 200 mB/s, 200 kW | up to 2 kW, and 20 mB/s of plain water |

The rack's window shows a **drop** before its state while water cools it: blue while it gets all it asks for, amber when it is short. Point at the drop to see the watts the water takes away and the flow.

## If something goes wrong

> [!PROBLEM] The drop is amber.
> The rack gets less cold water than it needs. Check that the CDU and the Chiller are on and fed, that the tower has water at its FILL inlet, and that the rack's outlet can drain: a full outlet stops the water too.

> [!PROBLEM] The Chiller stands still with hot water in it.
> It cools nothing without the tower's cold water to carry its heat away. Check the tower's loop and its FILL water.

> [!SEE] jsc:rack_cooling_and_heat, jsc:server_racks
