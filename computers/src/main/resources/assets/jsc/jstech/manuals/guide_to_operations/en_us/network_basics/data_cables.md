---
section: network_basics
order: 97
icon: jsc:ethernet_cable
covers_family: jsc:data_cables
---
# Data cables

The data cables are the network itself. They come in lines by the job they do, and each line has one cable for every era it exists in:

```table
{"caption": "The lines", "rows": [[{"words": "Access"}, {"words": "Small computers to a router"}], [{"words": "Backbone"}, {"words": "Routers, the Mainframe and the racks"}], [{"words": "Long distance"}, {"words": "Two networks, gateway to gateway"}], [{"words": "High compute"}, {"words": "A supercomputer's nodes to its HBW Interface"}], [{"words": "Crafting"}, {"words": "A Crafting Computer to the parts that feed its machines"}]]}
```

Table: The cable of each line, by era

| Era | Access | Backbone | Long distance | High compute |
| --- | --- | --- | --- | --- |
| Vintage | Thin Coaxial Cable | Thick Coaxial Cable | Telephone Line | none |
| Legacy | Ethernet Cable | HBW Cable | Leased Line | none |
| Transition | Cat 5e Cable | 10GBASE-CX4 Cable | T3 Line | InfiniBand Cable |
| Standard | Gigabit Ethernet Cable | Fibre Optic Cable | VLDC Cable | High Compute Cable |
| Advanced | Cat 6a Cable | OM5 Fibre Cable | Dark Fibre Cable | OSFP Cable |

The crafting line has one cable for all eras, the Crafting Cable. It is the only line that is not split by era.

```table
source: jsc:line_reach
caption: How far a run of each line reaches, in cables
heads: Line | Vintage | Legacy | Transition | Standard | Advanced
```

```table
source: jsc:line_speed
caption: How many items a tick each line moves
heads: Line | Vintage | Legacy | Transition | Standard | Advanced
```

Every cable has a speed and a range, and both count. A run longer than its range carries nothing, so whatever lies only beyond it is off the network. A cable's tooltip says how far one run of it reaches, and what starts a new run.

When you lay a run, count it against the range of its cable, and count the repeaters and routers you put in it. Those are the places where the range starts again.

Two rules govern two of the lines. From the Standard on, the backbone's fibre runs only straight, so it turns at an optical router ([](jsc:routers)). And the long distance line runs between exactly two ends and holds its block alone.

Fibre also goes into a Mainframe or a Cluster Management Computer only while that machine holds an Optical Network Card ([](jsc:expansion_cards)), and into a Server Rack only through a Fibre Optic Interface mounted in it ([](jsc:rack_equipment)).

## If something goes wrong

> [!PROBLEM] Fibre will not turn a corner.
> From the Standard on, fibre runs only straight. Turn it at an optical router.

> [!PROBLEM] A run carries nothing beyond its range.
> The run is longer than its range. Add a repeater or a router inside it, so that the cable after it starts counting again ([](jsc:routers)).
