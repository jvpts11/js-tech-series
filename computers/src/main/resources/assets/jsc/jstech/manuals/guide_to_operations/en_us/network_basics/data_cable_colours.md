---
section: network_basics
order: 98
icon: jsc:gigabit_cable
---
# Colours and lanes of the cables

Every block of cable holds up to nine wires. Each wire sits in one of nine lanes, laid out three across and three up, so one block can carry several lines at once.

Each line has its own lane, so a data cable, a peripheral cable and a crafting cable can share one block without joining.

Table: The lane of each line

| Line | Lane |
| --- | --- |
| Access | Top left |
| Backbone | Top middle |
| High compute | Middle |
| Long distance | Middle, alone in its block |
| Crafting | Right |
| Peripheral | Middle left |

You can dye a wire. Use a dye on the wire you are looking at, and that wire takes its colour. The sixteen colours of the game are all there.

Two wires of one line and era join when their colours agree: the same colour, or either of them with no colour. So an uncoloured wire joins every colour, and two wires of different colours stop at each other. Wires of different lines or eras never join, whatever their colour.

> [!DEFINE] Lane
> The place a wire takes inside a cable block. The lanes let several lines share one block.

## If something goes wrong

> [!PROBLEM] Two cables side by side do not join.
> They are of different lines or eras, or they are dyed in colours that do not agree. Check both: dye one to match the other, or leave both without dye.
