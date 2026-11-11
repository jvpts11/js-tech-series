---
section: autocrafting
order: 140
icon: jsc:crafting_cable
covers: [jsc:crafting_cable]
---
# How autocrafting works

You ask for 512 pistons. The network works out every step from what it has: how many planks, how much cobblestone, iron and redstone it needs, and what it must make first. It also decides on which machines and in which order. Then it runs the crafting table recipes and the machines, and puts the pistons in storage. That is autocrafting.

To do it, the network must know the recipes. A recipe it knows is a pattern, and there are three kinds: a bench pattern, a 3x3 grid like a crafting table's with its result; a machine pattern, what goes into a machine and what comes back; and a multi-stage pattern, steps of both kinds in order.

> [!DEFINE] Autocrafting
> The network making what it lacks, by running the recipes it knows on what it holds.

> [!DEFINE] Pattern
> A recipe the network knows: a bench grid, a machine's inputs and outputs, or several of those in order.

The parts are a Crafting Computer that runs the crafts ([](jsc:crafting_computers)), the Crafting Cable that joins it to the machines, and on that cable the parts that feed each machine. A Crafting Card in the computer is what lets it craft at all ([](jsc:crafting_cards)). Two programs you install yourself write the patterns and load them: the Pattern Studio and the Crafting Manager ([](jsc:pattern_studio), [](jsc:crafting_manager)).

```scene
{"balloons": [{"detail": "It runs the crafts and holds the Crafting Card.", "number": 1, "part": {"x": 0, "y": 0, "z": 1}, "text": "The Crafting Computer"}, {"number": 2, "part": {"x": 2, "y": 0, "z": 1}, "text": "The Crafting Cable"}, {"detail": "It holds the machine's patterns and feeds the machine.", "number": 3, "part": {"box": [6, 1, 1, 16, 15, 15], "x": 3, "y": 0, "z": 1}, "text": "The Crafting Interface"}, {"detail": "Put one against each input face of a machine that has several.", "number": 4, "part": {"box": [1, 1, 6, 15, 15, 16], "x": 4, "y": 0, "z": 0}, "text": "The Crafting Input Router"}, {"detail": "Put it against the machine's output.", "number": 5, "part": {"box": [1, 1, 0, 15, 15, 10], "x": 4, "y": 0, "z": 2}, "text": "The Crafting Receiving Bus"}, {"number": 6, "part": {"x": 4, "y": 0, "z": 1}, "text": "The machine"}], "blocks": [{"data": "{Hardware:{Size:15,Items:[{Slot:0,id:\"jsc:motherboard_atx_standard_1155\",count:1},{Slot:1,id:\"jsc:cpu_integra_celer_g530\",count:1},{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:6,id:\"jsc:crafting_card_pcie3\",count:1},{Slot:10,id:\"jsc:psu_850g\",count:1},{Slot:11,id:\"jsc:disk_ssd_500g\",count:1}]}}", "state": "jsc:crafting_computer[facing=south]", "x": 0, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,4144,0,0,0]}", "state": "jscore:cable", "x": 1, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,48,0,0,0]}", "state": "jscore:cable", "x": 2, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,28,0,0,0],Parts:[{Face:5b,Type:\"jsc:crafting_interface\",Data:{}}]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,40,0,0,0]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,36,0,0,0]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 2}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,16,0,0,0],Parts:[{Face:3b,Type:\"jsc:crafting_input_router\",Data:{}}]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,16,0,0,0],Parts:[{Face:2b,Type:\"jsc:receiving_bus\",Data:{}}]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 2}, {"state": "minecraft:furnace[facing=south]", "x": 4, "y": 0, "z": 1}], "caption": "The whole line: a Crafting Computer, the Crafting Cable, and a furnace with the parts that feed it and take back what it makes. Drag it to see the back.", "height": 130, "view": {"zoom": 1.35}}
```

> [!DEFINE] Crafting Cable
> The cable that joins a Crafting Computer to the machines it feeds, and carries the crafting parts on it.

> [!DEFINE] Crafting Card
> A card that goes in an expansion slot of a Crafting Computer, and sets how many interfaces it drives and how many recipes it keeps.

> [!DEFINE] Crafting Interface
> A cable part that sits against a machine, holds the machine's patterns and feeds the machine ([](jsc:crafting_interfaces)).

> [!DEFINE] Crafting Receiving Bus
> A cable part that sits against a machine's output and takes back what the machine makes for a craft.

Patterns are written in the Pattern Studio and carried to the Crafting Computer on media ([](jsc:pattern_encoders)). A craft then goes through the steps below.

Table: The steps of a craft

| Step | What happens |
| --- | --- |
| The request | The Mainframe starts a craft only while it is running and has an operating system. |
| The plan | The network works out the plan, and the craft waits until the plan is ready. |
| The computer | The plan goes to a Crafting Computer that is switched on and holds a Crafting Card. |
| One at a time | A Crafting Computer runs one craft at a time, unless the network has a Supercomputer. |
| The steps | Bench steps run on the Crafting Computer, from the recipes in its cards. |
| The machines | Machine steps run on the Crafting Interfaces that the computer drives. |
| The output | What a machine makes goes back to the network through the Crafting Receiving Bus tied to its interface. |

The network needs two things before any of it works: a running Mainframe with an operating system, and the recipes held in a card or in an interface. A machine works with the network when it offers item, fluid and chemical access, whatever mod it comes from.

## If something goes wrong

> [!PROBLEM] The plan says "nothing on the network knows how to make", and then the item's name.
> No pattern for that item is on the network. Add one, and load it into a card or an interface ([](jsc:patterns)).

> [!SEE] jsc:crafting_computers, jsc:crafting_interfaces, jsc:pattern_encoders, jsc:asking_for_a_craft
