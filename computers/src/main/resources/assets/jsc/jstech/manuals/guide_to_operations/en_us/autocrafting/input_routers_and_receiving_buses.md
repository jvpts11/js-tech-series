---
section: autocrafting
order: 146
icon: jsc:crafting_input_router
---
# Input routers and Receiving Buses

A Crafting Input Router sits on the cable that leaves a Crafting Interface, against one input face of a machine that has several. Its tooltip reads "On the cable that leaves a Crafting Interface, against one input face of a machine with several".

The interface sends each input of a pattern through the router chosen for it. A router moves nothing on its own: the interface feeds through it, and the interface measures and accounts for every item.

Each router has a filter, set the same way as the filters of the buses ([](jsc:buses)). The filter takes an item, or a tag, and the screen asks for "item, or #tag". A tag looks like "c:ores".

The default router for an input is the one whose filter takes it. The screen says "Chosen in the interface", so you can choose a different router in the interface.

Table: What the interface says about a router

| What the screen says | What it means |
| --- | --- |
| accepts here | The router takes this input for the machine on this face. |
| takes nothing here | The router takes no input for the face. |
| no machine on that face | Nothing is in front of the router. |
| is also taken by another router | Two routers take the same input, and the screen names the one that the interface uses. |
| Copper Ingot x3 for Bronze Ingot | A route: three Copper Ingots for the Bronze Ingot pattern. |

A Crafting Receiving Bus sits on the crafting cable against a machine's output. Its tooltip reads "On the crafting cable, against a machine's output; takes back what its interface's jobs made". It takes back what comes out of the machine and credits it to the craft that fed the machine.

```scene
{"balloons": [{"detail": "It stands against one input face of the machine.", "number": 1, "part": {"box": [6, 2, 2, 16, 14, 14], "x": 0, "y": 0, "z": 1}, "text": "The Crafting Input Router"}, {"detail": "It stands against the machine's output.", "number": 2, "part": {"box": [0, 2, 2, 10, 14, 14], "x": 2, "y": 0, "z": 1}, "text": "The Crafting Receiving Bus"}, {"number": 3, "part": {"x": 1, "y": 0, "z": 1}, "text": "The machine"}, {"number": 4, "part": {"x": 0, "y": 0, "z": 2}, "text": "The Crafting Cable"}], "blocks": [{"state": "minecraft:furnace[facing=south]", "x": 1, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,8,0,0,0],Parts:[{Face:5b,Type:\"jsc:crafting_input_router\",Data:{}}]}", "state": "jscore:cable", "x": 0, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,4,0,0,0]}", "state": "jscore:cable", "x": 0, "y": 0, "z": 2}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,8,0,0,0],Parts:[{Face:4b,Type:\"jsc:receiving_bus\",Data:{}}]}", "state": "jscore:cable", "x": 2, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:crafting_cable\"}],Links:[I;0,0,0,0,0,4,0,0,0]}", "state": "jscore:cable", "x": 2, "y": 0, "z": 2}], "caption": "An Input Router and a Receiving Bus on the crafting cable, each against a face of a furnace. Drag it to see them from behind.", "height": 110, "view": {"zoom": 1.2}}
```

The bus ties itself to the Crafting Interface that feeds its machine, either against it or through the routers of its own cable. You can also tie it by hand to one interface, or to several. Tying it by hand is how an output that goes into a chest beside the machine is handled, and by hand the bus takes up to 8 ties.

Table: The tie texts of a bus

| The text | What it means |
| --- | --- |
| by itself: the interface that feeds this machine | The bus found the interface on its own. |
| by hand | You tied the bus to the interface yourself. |
| TIE TO INTERFACE | The button that ties the bus to an interface by hand. |

While a job of its interfaces runs, what comes out of the machine is credited to that job, up to what the job fed in. What was in the machine before the job began stays there. A fluid or a chemical that no pattern lists stays in the machine as its buffer.

Table: The statuses on a bus

| Status | What it means |
| --- | --- |
| CREDITED | The output counts for the craft that fed the machine. |
| UNEXPECTED | The output is not on the pattern, so it goes to the network. |
| LATE | The job had already ended, so the output goes where the outputs of that job go. |

The routers and the buses are cable parts, so they mount on the crafting cable. An interface's own cable is coloured apart from the main crafting cable, which keeps the two runs apart ([](jsc:data_cables)).

## If something goes wrong

> [!PROBLEM] The interface says "no router on its cable takes it: this pattern cannot run".
> Put a Crafting Input Router on the interface's cable, against the input's face, with a filter that takes that input.

> [!PROBLEM] The screen says "Not on the cable of any interface: nothing uses it."
> Put the router on the cable that leaves an interface.

> [!PROBLEM] The screen says that an input is routed here, but the filter does not take it.
> Change the filter so that it takes that input, or pick another router.

> [!PROBLEM] The bus says "on no network".
> Put the bus on the crafting cable of a network, next to an interface.

> [!PROBLEM] The bus says "Not tied: no interface feeds this machine. Tie it to one by hand."
> Tie it to one interface by hand, with the TIE TO INTERFACE button.

> [!SEE] jsc:crafting_interfaces, jsc:buses, jsc:data_cables, jsc:when_a_craft_goes_wrong
