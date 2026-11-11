---
section: autocrafting
order: 147
---
# The Crafting Manager

The Crafting Manager is the program that loads recipes into the cards and the interfaces of a Crafting Computer, and that shows what its crafting parts are doing. It runs only on a Crafting Computer, and it does not come with any system. Install it from its CD, or on a Linux desktop from the Mirror, on a system with windows: Frames XP or newer, or a Linux, FreeBSD or UNIX desktop ([](jsc:programs)).

You need a Crafting Card to manage recipes, and only a Crafting Computer loads them ([](jsc:crafting_computers)).

The Crafting Manager has two tabs, "Recipes" and "Interfaces".

## The Recipes tab

The left pane lists the .craft files on a medium in a linked drive. With no medium in a linked drive, the pane reads "Removable media: none" and asks you to insert a disc into a linked drive. A medium with no pattern files reads "No .craft files".

The right pane lists the places where the computer keeps recipes: each card's ROM, and each interface with its machine recipes. Bench recipes go into a card's ROM, and machine and pipeline recipes go into an interface.

Table: The buttons of the Recipes tab

| Button | What it does |
| --- | --- |
| Load | Puts the selected recipe into the place chosen in "into", or into the first place with room. |
| Load all | Loads every recipe on the medium. |
| Download | Writes the recipes back onto the medium. |
| Remove | Removes the selected recipe. |
| Move to | Moves the selected recipe to another place. |

## Loading a recipe from a disc

1. Insert the disc with the .craft files into a linked drive.
2. Select the recipe in the left pane.
3. Choose the place in "into", or leave it on the first place with room.
4. Press Load.

## The Interfaces tab

The Interfaces tab has one row for each Crafting Interface on the cable. The summary line above the rows gives the interfaces that the cards drive, out of the interfaces on the cable, and the number of Crafting Cards.

Table: The columns of the Interfaces tab

| Column | What it shows |
| --- | --- |
| INTERFACE | The name of the interface. |
| MACHINE | The machine in front of the interface, or "Touches no machine" when nothing is in front. |
| PATTERNS | The recipes it holds, as two numbers: the recipes held, and the capacity. |
| MODE | "Exclusive" or "Not exclusive", as its modes ([](jsc:crafting_interfaces)). |
| STATE | "Idle" or "Paused". |
| JOBS | "Auto" when the limit is 0, otherwise the limit. |

Click the mode to switch it, and click the jobs to raise them by one. The jobs go up to 16 jobs, then wrap back to Auto, and a right-click lowers them. The interface's own screen allows up to 64 jobs. The Pause and Resume buttons pause and restart an interface.

Each card is listed with the interfaces it drives and how full its ROM is. A line reads "N more on the crafting cable wait, driven by none, until another card goes in" when some interfaces have no card to drive them.

## If something goes wrong

> [!PROBLEM] The screen says "A Crafting Card is required to manage recipes."
> Put a Crafting Card in the Crafting Computer.

> [!PROBLEM] The screen says "No Crafting Interface on this computer's crafting cable."
> Put a Crafting Interface on the crafting cable of this computer.

> [!PROBLEM] The screen says "No cards and no interfaces".
> Give the computer a Crafting Card, and put interfaces on its cable.

> [!PROBLEM] The screen says "Removable media: none".
> Insert a disc into a linked drive.

> [!PROBLEM] The screen says "No .craft files".
> The medium holds no pattern. Burn the pattern again, or insert the right medium.

> [!SEE] jsc:crafting_computers, jsc:crafting_interfaces, jsc:crafting_cards, jsc:drives, jsc:pattern_encoders, jsc:input_routers_and_receiving_buses
