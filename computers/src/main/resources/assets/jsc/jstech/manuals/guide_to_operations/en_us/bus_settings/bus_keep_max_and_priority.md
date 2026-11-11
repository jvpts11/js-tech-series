---
section: bus_settings
order: 127
---
# Keep, max, priority and mode

Five settings decide how much your bus moves and when. KEEP and MAX set the amounts. PRIORITY decides who goes first when buses compete. MODE and POWER decide whether a bus moves at all.

Table: The settings of a bus

| Setting | What it does |
| --- | --- |
| KEEP | How many stay in the chest on an Import Bus, or how many the chest is filled to on an Export Bus, from 0 to 1,000,000. |
| MAX | The most one move takes, where zero means as much as the speed allows in one second. |
| PRIORITY | Who goes first when two buses want the same thing, from -99 to 99 in the window, on Standard and later buses only. |
| MODE | CONTINUOUS moves at all times, and ON DEMAND moves only while the cable's block has a redstone signal. |
| POWER | ON lets the bus move, and OFF stops it. |

## KEEP and MAX

KEEP works in opposite ways on the two buses. An Import Bus leaves that many items in the chest it faces, and takes only what is above that number. An Export Bus fills the chest up to that number, and then stops.

MAX is the most one move takes, not a total. Zero means no cap: the bus moves as much as its speed allows in one second, which is 20 ticks. The window shows "any" with the note "a time" when MAX is zero.

## Priority

Each bus that wants something says so with its priority, every tick it wants it. A bus waits while a bus of a higher priority on the same network has wanted the same thing in the last 2 ticks. Equal priorities both go. Priority does not make a bus faster. It only decides who goes first ([](jsc:priorities)).

An Export Bus wants the item it sends. An Import Bus wants room in the network, and counts as wanting it while the network has had no room for its moves in the last 40 ticks.

## Mode and power

A bus on ON DEMAND checks its cable's block for a redstone signal. A bus on CONTINUOUS does not check. A bus set to OFF does not move, whatever its mode. MODE and POWER are on every Import and Export Bus, and not on the External Storage Bus ([](jsc:external_storage_bus)).

## Per-item keep and max

A Transition bus has no keep or max for the whole bus. Each item in its filter has its own keep and max, shown as a row in the window. An item with a keep or a max of zero uses the bus-wide value, and there is none on a Transition bus, so zero means no keep and no cap.

## If something goes wrong

> [!PROBLEM] The bus does not move at all.
> Check POWER: a bus set to OFF stops. Then check MODE: ON DEMAND needs a redstone signal on the cable's block.

> [!PROBLEM] A bus with a lower priority waits.
> A bus with a higher priority on the same network wanted the same thing a moment ago. Raise the priority of this bus, or lower the other one.

> [!PROBLEM] The window has no PRIORITY row.
> Only Standard and later buses have a priority. Vintage, Legacy and Transition buses do not.

> [!PROBLEM] The window shows a different number from the one you typed.
> The window keeps KEEP and MAX between 0 and 1,000,000, and PRIORITY between -99 and 99.

> [!SEE] jsc:bus_conditions, jsc:bus_filters_and_tags
