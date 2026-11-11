---
section: fab
order: 37
icon: jsc:machine_terminal
covers_family: jsc:building_chain_machines
---
# Building a chain machine

What a bench makes by hand, one part at a time, the chain's machines make in numbers: they crush and smelt the ore, roll and draw the metal, run the chemistry, grow the silicon, make the chips and press the boards and screens. Each machine is a multiblock you build block by block in the shape that is its own.

A machine is built of four kinds of block. Its casing, one of five by the work it stands: the Machine Casing for the mechanical machines, the Refractory Casing for heat, the Lined Casing for what corrodes, the Vacuum Casing for vacuum and gases, and the Clean Room Casing for the Lithography Stations. Its working blocks, which make it what it is: a Crusher's rolls, a Wafer Saw's wire web. Its entries, where things go in and come out. And the Machine Terminal, which runs it.

> [!DEFINE] Machine Terminal
> The control cabinet you place at a machine's terminal place: once the machine's whole shape stands round it, it becomes that machine's own terminal and runs it.

You tell the entries apart by their connector and their band: green takes in, orange gives out, yellow is energy, and the cyan Peripheral Port links a Lithography Station to its computer. The Item Inlet and Item Outlet take and give items, the Liquid Inlet and Liquid Outlet liquids, the Gas Inlet and Gas Outlet gases, and the Power Entry takes energy.

Build every block at its place, with the Machine Terminal at the terminal's, facing the way you want the machine's front. A [Structure Projector](jscore:structure_projector) shows you the shape before you build it. The terminal looks for a shape standing whole round it as you place it, as a block beside it changes and as you use it, and tells you when none stands.

Formed, the machine is drawn as one: its casing and its working blocks merge into its model, its entries draw nothing of their own, and its beacon turns while it works. It shows what it is working on, with the colours of what goes in and what comes out.

Use the terminal to open the machine's window, with its slots, its tanks and the energy it holds. Things go in through its inlets and come out through its outlets, from and to whatever moves them: a hopper, a pipe, the network's buses or a [Crafting Interface](jsc:crafting_interfaces), which runs it for the network's autocrafting.

Every machine but the Coke Oven draws its energy at low voltage through its Power Entry, and only while it works. The Coke Oven burns part of its own coal, as a real one does. On a server set to let its computers run free of energy, the machines run free too.

## If something goes wrong

> [!PROBLEM] The terminal says no machine stands round it.
> A block is missing, out of its place, or another stands where the shape wants its own: hold the shape against a Structure Projector's, from the machine's front.

> [!PROBLEM] The machine stands formed but does nothing.
> No energy reaches its Power Entry, what is in its inlets makes nothing it knows, or its outlets are full.

> [!PROBLEM] Breaking one block took the machine apart.
> Every block of the shape holds it together: put the block back and the terminal forms it again. Breaking the terminal gives you back the Machine Terminal.

> [!SEE] jsc:ore_and_metal_machines, jsc:lithography_stations
