---
section: autocrafting
order: 149
icon: jsc:crafting_computer
---
# Asking for a craft

You ask for a craft the way you ask for anything else: in the Network Interactor, or in IQL, the network's query language, with CRAFT 512 piston ([](jsc:iql)).

The network plans the craft from the bottom up: what it has, what it must make first, in which order and on which machines. Then it runs the steps. Bench steps run on the Crafting Computer, as many stages at once as its threads allow. Machine steps run on the Crafting Interfaces, each on its own machine ([](jsc:crafting_computers)).

Before you ask, the Craft Planner shows you the same plan, with what it needs and what is missing ([](jsc:craft_planner)). When you open a craft from the Network Interactor's catalogue, the popup asks for the plan at once.

## In the Network Interactor

Table: The keys of the craft popup

| Key | What it does |
| --- | --- |
| `Enter` | Submits the full craft. |
| `Backspace` | Divides the quantity by 10. |
| `Escape` | Closes the popup. |
| `Left` | Chooses another recipe, when the plan has more than one. |
| `Right` | Chooses another recipe, when the plan has more than one. |

The quantity, the number of items to craft, starts at 1 item and can go up to 99,999 items.

Table: The texts of the craft popup

| The text | What it tells you |
| --- | --- |
| Craft, then the item's name | The craft button, in the list. |
| Request, then a number of items | The button that requests that many items from storage. |
| RECIPE | How many patterns make the item, when it has more than one. |
| PLAN - with, then a recipe | The plan, with the recipe you chose. |
| PLAN - raw ingredients | The plan, when the item has no recipe to choose. |
| planning | The plan is being worked out. |
| EST (estimated time), then seconds | The estimated time of the craft, in seconds. |
| EST followed by two hyphens | There is no estimate yet. |
| Prio, then a level | The craft's priority, changed with the priority button: LOW, MED-, MED, MED+ or HIGH, with MED as the default. |

The computer remembers your choice of recipe for each item, so you choose once.

## In the terminal

The craft popup of the terminal has the buttons CRAFT and PARTIAL. CRAFT makes the whole craft. PARTIAL makes what the network can make now, and the popup shows that largest amount as "max now". CLOSE closes the popup.

The Craft tab of the terminal lists what is RUNNING and what is JUST MADE. It reads "nothing being made" when the network is idle. A pattern must be loaded before the terminal can craft: without one, the terminal reads "no patterns loaded" and asks you to load one from a medium under Patterns.

Table: The words of a finished craft

| Word | What it means |
| --- | --- |
| done | The craft made all that it asked for. |
| partial | The craft made part of what it asked for. |
| locked | Another operation held what the craft needed, so the craft timed out waiting. |
| failed | The craft could not be made. |

## Cancelling a craft

You cancel a craft in the Network Manager. Open the details of the craft there, and press Cancel. A craft cancelled while its plan is still being worked out is discarded ([](jsc:network_manager)).

## If something goes wrong

> [!PROBLEM] A many-step craft fails at one stage.
> That stage ran out of time: its machine has no power, is full, or never gave its output to the Receiving Bus. The craft names the stage ([](jsc:when_a_craft_goes_wrong)).

> [!PROBLEM] The craft shows "locked".
> Another operation held what it needed. Ask again once that operation has settled.

> [!SEE] jsc:iql, jsc:craft_planner, jsc:crafting_manager, jsc:network_manager, jsc:when_a_craft_goes_wrong, jsc:priorities, jsc:operation_states
