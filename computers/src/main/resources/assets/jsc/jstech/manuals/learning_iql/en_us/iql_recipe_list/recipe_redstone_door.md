---
section: iql_recipe_list
order: 403
icon: jsc:standard_redstone_interface
---
# A redstone door

A Redstone Interface lets a computer read a redstone signal, or send one. You place it, link it to the computer with a peripheral cable, and it reads the redstone beside it or emits a signal there ([](jsc:redstone_interfaces)).

> [!DEFINE] Redstone signal
> The on-or-off power of redstone, with a strength from 0 to 15. Strength 15 is the strongest.

A SET REDSTONE line makes the interface emit a strength. A door that reacts to redstone next to the interface receives that signal. A strength of 15 is the strongest, and 0 turns the signal off.

Table: What the recipe needs

| Need | What it takes |
| --- | --- |
| Era | Every era from Vintage to Advanced has a Redstone Interface |
| Program | None: you type the line at the prompt ([](jsc:where_to_type_iql)) |
| Link | A peripheral cable from the interface to the computer |

1. Place a Redstone Interface beside the door.
2. Link the interface to the computer with a peripheral cable ([](jsc:peripheral_cables)).
3. Name the interface in its own window. The default name is Redstone Interface, so give it a name you can type, such as Gate ([](jsc:redstone_interfaces)).
4. Type iql SET REDSTONE 'Gate' OUT 15 at the prompt, to send the strongest signal.
5. Type iql SET REDSTONE 'Gate' OUT 0 at the prompt, to turn the signal off.

Table: Lines of SET REDSTONE

| Line | What it does |
| --- | --- |
| `SET REDSTONE 'Gate' IN` | Reads the signal beside the interface. |
| `SET REDSTONE 'Gate' OUT 15` | Emits the strongest signal, 15. |
| `SET REDSTONE 'Gate' OUT 0` | Emits no signal. |

The reply names the interface and what it did, such as "Redstone Interface Gate emits 15". Quote the name in single quotes when it has a space.

## If something goes wrong

> [!PROBLEM] The reply reads "no Redstone Interface named Gate is linked to this machine".
> The name is spelled differently, or the interface is not linked to this computer. Check the name in its window, and the cable.

> [!PROBLEM] The reply reads "SET REDSTONE needs IN, or OUT and a strength from 0 to 15".
> You wrote OUT without a strength. Add a number from 0 to 15.

> [!PROBLEM] The reply reads "not a redstone strength, which runs from 0 to 15: 16".
> Use a strength from 0 to 15.

> [!PROBLEM] The reply reads "SET REDSTONE needs the interface's name, in quotes when it has spaces".
> Write the name right after SET REDSTONE, in single quotes when it has a space.

> [!PROBLEM] The interface reads and emits nothing.
> Its cable has no computer at its other end. Link the interface to a computer.

> [!SEE] jsc:redstone_interfaces, jsc:peripheral_cables, jsc:set_redstone, jsc:where_to_type_iql
