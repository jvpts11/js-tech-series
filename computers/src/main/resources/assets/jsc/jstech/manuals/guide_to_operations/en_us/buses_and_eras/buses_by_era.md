---
section: buses_and_eras
order: 125
---
# Buses by era

Each era of hardware has its own buses, and the newer buses carry more. You get each bus from the creative tabs, as with everything in this mod. Exa and Singularity use the Advanced bus.

Table: Bus items by era

| Era | Import Bus | Export Bus | External Storage Bus |
| --- | --- | --- | --- |
| Vintage | Vintage Import Bus | Vintage Export Bus | Vintage External Storage Bus |
| Legacy | Legacy Import Bus | Legacy Export Bus | Legacy External Storage Bus |
| Transition | Transition Import Bus | Transition Export Bus | Transition External Storage Bus |
| Standard | Import Bus | Export Bus | External Storage Bus |
| Advanced | Advanced Import Bus | Advanced Export Bus | Advanced External Storage Bus |

The newer the era, the more you can set on the bus, and the faster it moves. The table shows the speed of each era's bus and what its window can set.

```table
{"caption": "Speed and features by era", "heads": ["Era", "Speed", "What the window can set"], "rows": [[{"words": "Vintage"}, {"amount": 1, "unit": "it/t"}, {"words": "No filter: an Export Bus sends the one item in its cell, and an Import Bus takes what it finds first."}], [{"words": "Legacy"}, {"amount": 8, "unit": "it/t"}, {"words": "A filter of up to 5 items, ONLY THESE or ALL BUT THESE, and one keep and one max for the whole bus."}], [{"words": "Transition"}, {"amount": 16, "unit": "it/t"}, {"words": "A filter of up to 5 items, each with its own keep and max."}], [{"words": "Standard"}, {"amount": 32, "unit": "it/t"}, {"words": "A filter, a keep and a max for the whole bus, a priority and conditions."}], [{"words": "Advanced"}, {"amount": 64, "unit": "it/t"}, {"words": "Everything Standard has, plus up to 4 tags and loose matching."}]]}
```

> [!DEFINE] Filter
> The list of up to five items, fluids or chemicals that a bus is told to move.

> [!DEFINE] Tag
> A group of items with a shared name, such as c:ores. A bus can list a tag in place of single items.

> [!DEFINE] Keep
> How many items a bus leaves in the block it faces (Import), or fills that block up to (Export).

> [!DEFINE] Max
> The most items one move takes. Zero means as many as the speed allows.

A bus's speed is the lower of two numbers: what its era allows, and what its cable carries. A Standard Import Bus on a Standard access cable moves 32 items a tick. On a Legacy access cable it moves 16 items a tick, because that cable carries 16 items a tick. A Vintage bus moves 1 item a tick on any cable.

```table
{"caption": "What a cable carries", "heads": ["Era", "Access line", "Backbone line"], "rows": [[{"words": "Vintage"}, {"amount": 4, "unit": "it/t"}, {"amount": 16, "unit": "it/t"}], [{"words": "Legacy"}, {"amount": 16, "unit": "it/t"}, {"amount": 64, "unit": "it/t"}], [{"words": "Transition"}, {"amount": 64, "unit": "it/t"}, {"amount": 256, "unit": "it/t"}], [{"words": "Standard"}, {"amount": 128, "unit": "it/t"}, {"amount": 512, "unit": "it/t"}], [{"words": "Advanced"}, {"amount": 256, "unit": "it/t"}, {"amount": 2048, "unit": "it/t"}]]}
```

Storage buses do not mount on long-distance or HPC lines. A cable of a newer era carries more, so a newer cable lifts the speed of a bus on it.

The External Storage Bus has no speed of its own. The network reaches its inventory at one tenth of what the cable carries, and at least 1 item a tick. It has no POWER and no MODE setting. Its own page explains it ([](jsc:external_storage_bus)).

## If something goes wrong

> [!PROBLEM] The bus moves fewer items than the table says.
> The cable is the limit. Use a cable of a newer era, or an access or backbone line ([](jsc:data_cables)).

> [!SEE] jsc:buses, jsc:eras
