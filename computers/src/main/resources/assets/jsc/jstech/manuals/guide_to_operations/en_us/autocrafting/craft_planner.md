---
section: autocrafting
order: 148
---
# The Craft Planner

The Craft Planner is a program that shows you a craft before you make it. You pick an item from the network's craft catalogue. It then shows the whole plan: each step, each ingredient and the number of stages.

The network works out the catalogue and the plan, so they show what the network holds now. The Craft button sends the same request as the terminal and the Network Interactor ([](jsc:asking_for_a_craft)).

## Planning a craft

1. Type part of the item's name in the search box.
2. Pick the item from the list.
3. Read the plan: the steps, the stages and the ingredients, with what the network has.
4. Press Craft to start the craft.

Table: The texts of the Craft Planner

| The text | What it means |
| --- | --- |
| search item | The box where you type to find an item in the catalogue. |
| no match | The search finds no item of that name. |
| Pick an item to plan. | Nothing is chosen yet, so there is no plan. |
| planning | The plan is being worked out. |
| No pattern on the network makes this. | No recipe is known for the item, so there is no plan. |
| Craftable | The network can make the item from what it holds now. |
| max, an amount, and a stage count | The largest amount the network can make now, and the number of stages it takes. |
| Steps and Tree | Two views of the plan: the steps in order, and the craft tree. |
| INGREDIENTS | The raw ingredients, each with the amount needed and what the network has. |
| have 12 | The network holds 12 of that ingredient. |
| need 16, have 15, in red | The network holds 15 of the 16 the plan needs of that ingredient. |
| raw | The ingredient is a raw material, with no recipe of its own. |
| machine x3, bench x2 | How many times a step runs on a machine, and how many times it runs on a bench. |
| Craft, then the item's name | The button that starts the craft. |
| Missing 1, in red | The network lacks 1 of the ingredients, named beside it. Craft stays grey until the network holds them all. |
| Print | Prints the plan on the computer's printer. |

The planner works from what the network holds now, so "max" is the most the network can make at this moment. It looks only so deep: a recipe nested more than 16 levels down is not planned. A recipe that depends on itself is cut, so the plan does not loop.

Print sends the plan to the computer's printer as a shopping list: the target, the stages, each ingredient with what you need and what the network holds, and what is missing, each missing line on a grey band, with the machine and the time it was printed.

## If something goes wrong

> [!PROBLEM] The screen says "No pattern on the network makes this."
> Add a pattern for the item, as a bench, machine or multi-stage pattern ([](jsc:patterns)).

> [!PROBLEM] An ingredient shows what the plan needs and what the network has, in red.
> Stock more of that ingredient, and plan the craft again.

> [!SEE] jsc:asking_for_a_craft, jsc:patterns, jsc:when_a_craft_goes_wrong, jsc:crafting_manager
