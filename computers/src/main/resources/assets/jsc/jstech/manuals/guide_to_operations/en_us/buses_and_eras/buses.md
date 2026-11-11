---
section: buses_and_eras
order: 124
icon: jsc:import_bus
covers_family: jsc:buses
---
# Buses

Buses are thin parts you put on the face of a data cable, against a chest or a machine, so the network can reach it without you.

```scene
{"balloons": [{"detail": "It takes items from the chest above it and puts them into the network.", "number": 1, "part": {"box": [2, 11, 2, 14, 16, 14], "x": 1, "y": 0, "z": 1}, "text": "The Import Bus"}, {"detail": "It takes items from the network and puts them into the furnace above it.", "number": 2, "part": {"box": [2, 11, 2, 14, 16, 14], "x": 5, "y": 0, "z": 1}, "text": "The Export Bus"}, {"number": 3, "part": {"x": 1, "y": 1, "z": 1}, "text": "The chest"}, {"number": 4, "part": {"x": 5, "y": 1, "z": 1}, "text": "The furnace"}, {"detail": "It carries the buses' work to the network.", "number": 5, "part": {"x": 3, "y": 0, "z": 1}, "text": "The data cable"}], "blocks": [{"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;32,0,0,0,0,0,0,0,0],Parts:[{Face:1b,Type:\"jsc:import_bus\",Data:{}}]}", "state": "jscore:cable", "x": 1, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;48,0,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 2, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;48,0,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;48,0,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;16,0,0,0,0,0,0,0,0],Parts:[{Face:1b,Type:\"jsc:export_bus\",Data:{}}]}", "state": "jscore:cable", "x": 5, "y": 0, "z": 1}, {"state": "minecraft:chest[facing=south]", "x": 1, "y": 1, "z": 1}, {"state": "minecraft:furnace[facing=south,lit=false]", "x": 5, "y": 1, "z": 1}], "caption": "An Import Bus on one cable face pulls items out of a chest, and an Export Bus on another face fills a furnace. The cable between them joins both to the network. Drag it to look at the buses from the side.", "height": 120, "view": {"pitch": 30.0, "zoom": 1.35}}
```

> [!DEFINE] Bus
> A thin part on one face of a data cable that moves items between the network and the block it touches.

Every move a bus makes is an Operation ([](jsc:what_operations_are)) that the network's Mainframe runs. So no bus moves while its network has no Mainframe ([](jsc:mainframes)).

There are three kinds of bus. Each one reaches one block, the chest, tank or machine it faces.

Table: The kinds of bus

| Kind | What it does |
| --- | --- |
| Import Bus | Takes items from the block it faces and puts them into the network. |
| Export Bus | Takes items from the network and puts them into the block it faces. |
| External Storage Bus | Moves nothing itself: the network uses the inventory it faces as storage of its own. |

> [!DEFINE] Import Bus
> A bus that takes items from the block it faces and puts them into the network.

> [!DEFINE] Export Bus
> A bus that takes items from the network and puts them into the block it faces.

Buses move fluids too ([](jsc:fluids)). The External Storage Bus has its own page ([](jsc:external_storage_bus)).

## Putting a bus on a cable

1. Hold the bus in your hand.
2. Right-click the data cable on the face where the bus should sit.
3. Right-click the placed bus to open its window ([](jsc:bus_window)).

If you point at the chest or the machine instead, the bus looks for a cable behind that face first, then for any cable next to it.

A bus mounts only on an access or a backbone cable of its own era or of an earlier one ([](jsc:data_cables), [](jsc:eras)). On any other cable it does not fit. A face that already holds a part, or that a wire crosses, takes no bus, and nothing happens.

## When a bus moves

A bus moves only when every requirement in this table holds. Check them in the bus's window when a bus stays still.

Table: What a bus needs before it moves

| Requirement | What it means |
| --- | --- |
| The cable is on a network | A cable on no network moves nothing, and the window's lamp says OFFLINE. |
| The network has a Mainframe | Without a Mainframe, no bus moves. |
| The bus is ON | A bus set to OFF stays still until you turn it ON. |
| Its mode allows it | A bus on ON DEMAND moves only while its cable's block has a redstone signal. |
| Its conditions hold | A condition that does not hold makes the bus wait. |

> [!DEFINE] Condition
> A rule a bus checks before it moves, such as a stock count or an hour of the day.

A bus that has moved something in the last 60 ticks, which is 3 seconds, shows a busy model on its cable, and that model blinks.

## If something goes wrong

> [!PROBLEM] The message "Storage buses mount on access and backbone cables of their era or an earlier one" appears on the action bar.
> Use an access or a backbone cable of the bus's era or of an earlier era ([](jsc:data_cables)).

> [!PROBLEM] Right-clicking the cable does nothing, and no message appears.
> The face already holds a part, or a wire crosses it. Pick another face of the cable.

> [!PROBLEM] The bus moves nothing.
> Check the requirements in the table above. The Activity tab shows the holds the bus had, with their reasons ([](jsc:bus_activity)).

> [!SEE] jsc:bus_filters_and_tags, jsc:buses_by_era
