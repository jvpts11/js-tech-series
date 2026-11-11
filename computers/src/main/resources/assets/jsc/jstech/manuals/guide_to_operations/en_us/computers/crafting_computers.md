---
section: computers
order: 51
icon: jsc:crafting_computer
covers_family: jsc:crafting_computers
---
# Crafting Computers

The Crafting Computer crafts for the network. It runs the crafting table recipes itself, and it drives the machines that make the rest through [Crafting Interfaces](jsc:crafting_interfaces).

It crafts only with a Crafting Card in it: without one, it does not craft at all. The card decides how many Crafting Interfaces it drives, and it keeps the crafting table [patterns](jsc:patterns) in its own memory, so they move with the card ([](jsc:crafting_cards)).

Build it as you would a Personal Computer ([](jsc:personal_computers)). It takes the same kinds of part, and it holds these:

```table
{"caption": "What a Crafting Computer holds", "rows": [[{"words": "Motherboard"}, {"literal": "1"}], [{"words": "Processors"}, {"literal": "1"}], [{"words": "Memory"}, {"literal": "4"}], [{"words": "PCI Express cards"}, {"literal": "4"}], [{"words": "Disks"}, {"literal": "2"}], [{"words": "Power supply"}, {"literal": "1"}], [{"words": "Drive bays"}, {"literal": "2"}]]}
```

The era of the Crafting Card sets how many Crafting Interfaces it drives, and how many patterns its memory keeps:

Table: Crafting Cards by era

| Era | Crafting Interfaces | Patterns on the card |
| --- | --- | --- |
| Vintage | 2 | 2 |
| Legacy | 4 | 4 |
| Transition | 5 | 5 |
| Standard | 6 | 6 |
| Advanced | 8 | 8 |

Open the Crafting Manager on it to load its patterns and watch the jobs ([](jsc:crafting_manager)).

## If something goes wrong

> [!PROBLEM] It crafts nothing.
> It has no Crafting Card, or it is off. Put a card in it, and switch it on.

> [!PROBLEM] A crafting table pattern is gone.
> It lives on the card. It went with the card to another computer, so put the card back.

> [!SEE] jsc:autocrafting
