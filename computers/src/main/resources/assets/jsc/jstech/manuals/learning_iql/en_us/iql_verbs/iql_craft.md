---
section: iql_verbs
order: 369
---
# CRAFT

CRAFT asks the network to make items from its recipes. You do not make the items by hand. The network uses the patterns it has learned ([](jsc:patterns)) and the machines that carry them out.

```iql torches.iql
CRAFT 64 torch
```

CRAFT needs an engine running on the Mainframe, because an engine works out how to make the craft. Without one, the network refuses the statement. What an [engine](jsc:iql_engines_overview) is, and how you start one, is explained there.

1. Type CRAFT, a quantity and the item, such as CRAFT 64 torch.
2. Press [[Enter]], then read the reply.

Table: The replies of CRAFT

| Reply | What it means |
| --- | --- |
| `CRAFT queued: <quantity> <item>` | The craft has started. |
| `Network Operations Service unavailable` | No engine runs on the Mainframe. |
| `the network has no running Mainframe` | The network has no Mainframe running. |
| `no pattern crafts <item>` | No recipe on the network makes the item. |
| `unknown item: <name>` | The name is not an item. |

With no quantity, the reply shows "all" in place of the number. PRIORITY works on a CRAFT as it does on the other actions ([](jsc:clauses_priority_if_with)). FROM and TO are read, but CRAFT does not use them. The recipes are the patterns the network has learned, such as those a Crafting Interface holds ([](jsc:crafting_interfaces)).

## If something goes wrong

> [!PROBLEM] Network Operations Service unavailable
> Start an engine on the Mainframe ([](jsc:iql_engines_overview)).

> [!PROBLEM] no pattern crafts <item>
> Teach the network a recipe with a Pattern Encoder ([](jsc:pattern_encoders)), then craft again.

> [!PROBLEM] unknown item: <name>
> Check the spelling, and use the item's id, such as iron_ingot.

> [!SEE] jsc:asking_for_a_craft, jsc:crafting_interfaces
