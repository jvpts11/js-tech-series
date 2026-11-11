---
section: bus_windows
order: 129
icon: jsc:import_bus
---
# A bus's window

Right-click a placed bus to open its window. The title in capitals at the top says which bus it is, such as IMPORT BUS or EXTERNAL STORAGE BUS. A lamp at the top right shows the state of its cable, and your own inventory is on the same screen.

Table: The tabs of the window

| Tab | What it shows |
| --- | --- |
| CONFIGURE | The name of the bus and its settings, row by row. |
| ACTIVITY | The moves and holds of the bus, explained on the page What a bus did. |
| SOFTWARE | The settings written as IQL and SIGMA lines, and which program set what. |

## The lamp

The lamp is green when the bus's cable reaches a network. Hover it and it says LINKED. It is red when the cable reaches no network, and then it says OFFLINE.

## CONFIGURE

The NAME field comes first. Type a name, up to 32 characters, and the change reaches the server at once. Software finds the bus by this name, so give each bus one ([](jsc:programming_buses)).

Below the name, the rows show the settings your bus's era has. Vintage shows the fewest, and Advanced shows the most. Each row has a label, its controls and sometimes a note.

Table: How you change a row

| Kind of row | How you change it |
| --- | --- |
| Filter cells | Click a cell with an item in your hand to list it. With an empty hand, click it to clear it. |
| Option words, such as ONLY THESE or ON DEMAND | Click the option you want. |
| Numbers: KEEP, MAX and PRIORITY | Click the - or + buttons: PRIORITY changes by 1, and KEEP and MAX change by 16 when Shift is held. |
| Conditions and tags | Click the + button to add one, and click a tag's chip or the right end of a condition's row to take it off. |

## SOFTWARE

The SOFTWARE tab writes the bus's settings as IQL statements and as SIGMA calls, so you can copy them into a program. ADDRESS shows the name a program uses, such as bus://Ore in. The section SET BY PROGRAMS lists which program set which setting, with lines such as "Sigma: the keep".

> [!DEFINE] Sigma
> A programming language of the mod. Its calls set buses the same way IQL statements do.

Hover a setting that a program set, and it says which program set it. Changing the setting in the window takes it back by hand.

## If something goes wrong

> [!PROBLEM] A program cannot find the bus.
> The bus has no name, or the program uses another one. Give the bus the name the program uses, in CONFIGURE. ADDRESS says "Give the bus a name in CONFIGURE so software can find it." when the name is missing.

> [!PROBLEM] The lamp says OFFLINE.
> The bus's cable is on no network. Join the cable to a network ([](jsc:first_network)).

> [!PROBLEM] A setting is not the one you set.
> A program set it. Hover the setting to see which program, and change it by hand to take it back.

> [!SEE] jsc:bus_activity, jsc:programming_buses
