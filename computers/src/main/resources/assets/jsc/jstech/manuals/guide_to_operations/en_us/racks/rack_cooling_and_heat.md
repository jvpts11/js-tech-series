---
section: racks
order: 59
---
# Cooling and heat

A rack has one temperature for the whole cabinet, shared by every machine in it. When you fill a rack it heats up, and past a point its machines slow down. Every Cooling Unit and every Vent Panel in the rack counts, wherever it sits.

> [!DEFINE] Heat budget
> The watts of heat that a rack takes away, at which it stands at 80 °C.

The heat budget is 1,000 W, plus what each Cooling Unit adds, by its era, plus 500 W for each Vent Panel. The load is the power draw of every machine whose bay is on: its processors and cards, added up.

```table
source: jsc:cooling_units
caption: Cooling Units
heads: Unit | Era | Adds to the heat budget
```

The rack's temperature follows the same rules as a computer's ([](jsc:cooling)): the surroundings plus 60 °C times the load over the heat budget. Up to 80 °C every machine runs at full speed. From 90 °C every machine slows down by the same share, just enough to hold the rack at 90 °C, and never below 25%. At 100 °C, when not even 25% holds the heat, every machine in the rack shuts down at once, and no bay switches on until the rack has cooled below 60 °C.

Table: Some loads, in the plains

| Load | Heat budget | Temperature | Share of speed |
| --- | --- | --- | --- |
| 1,000 W | 1,000 W | 80 °C | 100% |
| 1,500 W | 1,000 W | 90 °C | 66% |
| 2,000 W | 1,000 W | 90 °C | 50% |
| 4,000 W | 1,000 W | 91 °C | 25%, the lowest share |
| 5,000 W | 1,000 W | 108 °C | Thermal trip: every machine shuts down |
| 5,000 W | 2,500 W, with one Standard Cooling Unit | 90 °C | 50% |

The rack screen shows the temperature, and THROTTLED with the share while its machines slow down. The slowdown cuts the capacity of each server, including the bonus from its graphics cards, and wears the servers' power supplies twice as fast.

## If something goes wrong

> [!PROBLEM] The rack shows THROTTLED.
> Switch off the bays of the machines you do not need, or add a Cooling Unit or a Vent Panel ([](jsc:rack_equipment)).

> [!PROBLEM] Every server in the rack went dark at once.
> The rack tripped at 100 °C. Wait until it has cooled below 60 °C, and add cooling or take load out before you switch the bays on again.

> [!SEE] jsc:rack_equipment, jsc:server_racks, jsc:cooling
