---
section: autocrafting
order: 143
---
# The Pattern Studio

The Pattern Studio is the desktop program in which you write patterns. It keeps three drafts on the computer, one for each tab: "Bench", "Machine" and "Multi-stage". A draft is not a recipe until it is complete, so you can leave one half written and come back to it.

The Studio does not come with any system. Install it from its CD, or on a Linux desktop from the Mirror, on a system with windows: Frames XP or newer, or a Linux, FreeBSD or UNIX desktop ([](jsc:programs)).

Nothing is used up in the Studio. A click on a cell of the editor records a copy of what your cursor holds, and your inventory sits in a band under the editor.

The Load button works only on a Crafting Computer, because only that computer loads recipes ([](jsc:crafting_computers)).

Table: The tabs of the Studio

| Tab | What it writes |
| --- | --- |
| Bench | A bench pattern: a 3x3 grid and its result. |
| Machine | A machine pattern: its inputs, its outputs and its timeout. |
| Multi-stage | A pipeline of stages, from your drafts or from files on a medium. |

On the right, the rail lists the files on the drives of the computer and the linked Pattern Encoder, under the tabs "Files" and "Encoder". The hints of the rail read "Click adds a stage" and "Click opens the file".

## The buttons

Table: The buttons of the Studio

| Button | What it does |
| --- | --- |
| \+ Bench | Adds the bench draft as a stage of the multi-stage tab. The Studio answers "Bench stage added". |
| \+ Machine | Adds the machine draft as a stage. The Studio answers "Machine stage added". |
| Burn | Writes the draft onto the medium of the linked Pattern Encoder ([](jsc:pattern_encoders)). |
| Save to disk | Writes the draft to the system disk of the computer. The Studio answers "Saved" and the file's name. |
| Load | Loads the draft into the ROM of this Crafting Computer, where bench recipes are kept ([](jsc:crafting_cards)). |

## Writing a bench pattern

1. Open the Bench tab.
2. Hold an item on your cursor, and click a cell of the grid to record a copy of it.
3. Fill the grid until it makes a result. The preview shows what the network would use.
4. Type a name in the Name field.
5. Press Load, or Save to disk, to keep the draft.

The Name and Note fields name the pattern, up to 64 and 256 characters. The Timeout field sets how many ticks a machine pattern waits, and it is at least 1 tick.

The preview shows what the network would use for the draft. Its lines read "Network would use:" and "In stock:". A cell that takes a tag shows "Any #" and the tag.

Until you add a stage, the multi-stage tab reads "No stages yet". Each stage then shows its number, its kind in brackets, "[bench]" or "[machine]", and its name.

If Just Enough Items (JEI), the recipe viewer, is installed, the transfer button of a recipe sends it into the open Studio. A bench recipe goes to the bench draft, and any recipe with inputs and outputs goes to the machine draft.

The Studio answers "Recipe placed on the bench" or "Recipe placed in the machine draft". If the Studio is not open, the message is "Open the Pattern Studio to transfer recipes".

## If something goes wrong

> [!PROBLEM] The screen says "The bench draft is not a recipe".
> The grid makes no result. Fill it so that one exists.

> [!PROBLEM] The screen says "The machine draft needs an input and an output".
> Add at least one input and one output.

> [!PROBLEM] The Studio says "Not a machine recipe" or "Not a pipeline".
> The file is of the wrong kind for this tab. Open a file of the kind the tab takes.

> [!SEE] jsc:patterns, jsc:pattern_encoders, jsc:crafting_manager, jsc:crafting_cards
