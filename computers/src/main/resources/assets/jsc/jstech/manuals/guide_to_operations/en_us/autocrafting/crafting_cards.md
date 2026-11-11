---
section: autocrafting
order: 141
icon: jsc:crafting_card_pcie3
covers_family: jsc:crafting_cards
---
# Crafting Cards

The Crafting Card is the part that lets a Crafting Computer craft. You put it in an expansion slot of the computer. Without a card the computer does not craft at all ([](jsc:crafting_computers)).

A card sets how much its computer can do. It drives a number of Crafting Interfaces, and it keeps a number of bench recipes in its own memory. The recipes stay on the card, so they go with it to another computer.

> [!DEFINE] ROM
> The memory of a Crafting Card where its bench recipes are kept. It is part of the card, so the recipes move with it.

Table: What one card gives, by era

| Era | Interfaces it drives | Recipes its ROM keeps |
| --- | --- | --- |
| Vintage | 2 | 2 |
| Legacy | 4 | 4 |
| Transition | 5 | 5 |
| Standard | 6 | 6 |
| Advanced | 8 | 8 |

The two numbers are separate limits. Today they match in every era, but they measure different things: the interfaces a card drives, and the recipes it keeps.

A computer with several cards adds them up: the interfaces it drives and the recipes it keeps are the totals over all its cards. A computer that is off drives no interfaces.

A card also sets the speed. Each card adds its throughput, and the craft speed of the computer is its processor (CPU) capacity times the sum of those throughputs.

Each card also adds its threads. The threads of a computer are the sum of the threads of its cards: the number of stages it runs in parallel for one craft.

> [!DEFINE] stage
> One step of a pipeline: a bench pattern or a machine pattern.

Table: The Crafting Cards

| Card | Era | Throughput | Threads |
| --- | --- | --- | --- |
| Forge Logic Crafting Card ISA | Vintage | 0.02x CPU | 1 |
| Forge Logic Crafting Card PCI | Legacy | 0.03x CPU | 2 |
| Forge Logic Crafting Card | Transition | 0.05x CPU | 2 |
| Forge Logic Crafting Card T3 | Transition | 0.10x CPU | 4 |
| Forge Logic Crafting Card PCI-e 3.0 | Standard | 0.15x CPU | 6 |
| Forge Logic Crafting Card T4 | Advanced | 0.20x CPU | 8 |

A card goes into a slot of its own bus. The PCI card also goes into an AGP or a PCI-e board, as the boards of its years had PCI slots beside the graphics slot: the Legacy boards are AGP or PCI-e.

Hover over a card to read its tooltip. Each line of the tooltip tells you one thing.

```table
{"caption": "The tooltip of a card", "heads": ["Line of the tooltip", "What it tells you"], "rows": [[{"words": "Crafting accelerator (FPGA)"}, {"words": "The kind of card: a crafting accelerator, built on a field-programmable gate array (FPGA)."}], [{"words": "Threads  -  N threads"}, {"words": "How many stages the card runs at once."}], [{"words": "Throughput  -  Nx CPU"}, {"words": "The card's speed as a multiple of the computer's processor (CPU) capacity."}], [{"words": "Drives N Crafting Interfaces"}, {"words": "How many interfaces the card drives."}], [{"words": "ROM  -  N of M bench recipes"}, {"words": "How many bench recipes the card keeps, out of how many it can keep."}], [{"words": "Recipe lines, then \"and N more\""}, {"words": "The names of up to 8 bench recipes, then how many are left out."}]]}
```

You load a bench recipe with the Load button of the Pattern Studio or in the Crafting Manager ([](jsc:crafting_manager)). It goes into the first card with room, counting from the first slot, and a card that already keeps the same recipe is not loaded twice.

## If something goes wrong

> [!PROBLEM] The Crafting Computer crafts nothing.
> It has no Crafting Card, or it is off. Give it a card and switch it on.

> [!PROBLEM] A bench recipe is not on the computer any more.
> The card that kept it went to another computer with the recipe. Take the card back, or load the recipe again.

> [!PROBLEM] The screen says "Only a Crafting Computer loads recipes".
> Load the recipe into a Crafting Computer.

> [!PROBLEM] The screen says "A Crafting Card is required".
> Put a card in the Crafting Computer first.

> [!PROBLEM] The screen says "Not loaded: a card keeps it already, or every card's ROM is full".
> A card keeps the recipe already, so nothing is needed. If every ROM is full, add a card, or remove a recipe in the Crafting Manager.

> [!SEE] jsc:crafting_computers, jsc:crafting_manager, jsc:crafting_interfaces
