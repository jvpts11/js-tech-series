---
section: mod_integrations
order: 162
---
# Create

Create is a mod of machines that turn, and of contraptions: blocks that a bearing, a piston, a pulley or a train carries off and sets down again. Create is optional.

## Computers on contraptions

A contraption can carry a Personal Computer, a Crafting Computer or another small computer. The computer comes down switched off where the contraption sets it, with its system, its parts and its files. Switch it on again when you want it.

A computer that copies files or installs a system is not carried until it is done. If the contraption starts from it, or moves into it, Create says the block cannot be moved and the contraption does not assemble. If it is only glued on beside, it stays where it is.

The big machines never ride a contraption. The Mainframe, the Server Rack, the Mining Computer and the WAN Gateway stay where they stand.

## Computers on ships

Create Aeronautics builds ships that fly with their blocks. A computer aboard keeps running in flight. A ship talks to the ground by Wi-Fi: a computer aboard with a Wi-Fi card joins an access point on the ground within its reach, and an access point aboard reaches computers on the ground ([](jsc:wireless)). A data cable never crosses between them.

## Create's machines on the network

With J's Industrial installed as well, its industrial control cable reads Create's turning blocks, such as a motor, a shaft or a Rotation Speed Controller. The network lists them in the create_kinetics table: each block with its speed, the stress on its kinetic network, the capacity of that network, and whether it is overstressed.

Table: Create's machines in IQL

| Statement | What it does |
| --- | --- |
| `QUERY create_kinetics` | Lists every Create block the network's computers read. |
| `UPDATE create_kinetics SET target_speed = 64` | Aims every Rotation Speed Controller it keeps at 64 RPM. |

Clutches and gearshifts are worked by redstone. Use a Redstone Interface for them ([](jsc:redstone_interfaces)).

> [!DEFINE] Contraption
> A group of blocks that Create moves together and sets down again.

> [!DEFINE] RPM
> Turns a minute: how fast a Create block turns.

> [!DEFINE] Stress unit (su)
> Create's measure of how much a kinetic network carries and how much it can carry.

## If something goes wrong

> [!PROBLEM] A bearing does not assemble, and Create says a computer cannot be moved.
> The computer is copying files or installing a system. Wait until it is done, then start the bearing again.

> [!PROBLEM] The create_kinetics table is empty.
> A computer of the network must read the blocks. Link one with a fieldbus card to them through the industrial control cable.

> [!PROBLEM] The network says "the create_kinetics table is only there with Create".
> Create is not installed, or its version is not one J's Computers works with. The log says which.

> [!SEE] jsc:wireless, jsc:redstone_interfaces, jsc:update_cards
