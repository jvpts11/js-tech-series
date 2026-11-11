---
section: iql_settings
order: 377
---
# SET REDSTONE

A [Redstone Interface](jsc:redstone_interfaces) joins the network to redstone signals in the world. SET REDSTONE tells it whether to read a signal or to emit one, and at what strength.

Table: The two forms of SET REDSTONE

| Statement | What it does |
| --- | --- |
| `SET REDSTONE 'x' IN` | The interface reads a signal. |
| `SET REDSTONE 'x' OUT 15` | The interface emits a signal of the strength you type, from 0 to 15. |

The strength is a whole number from 0 to 15. The name you type must be one of the interfaces this computer is linked to.

Table: The replies of SET REDSTONE

| Reply | What it means |
| --- | --- |
| `Redstone Interface <name> reads` | The interface reads a signal now. |
| `Redstone Interface <name> emits <strength>` | The interface emits that strength now. |
| `no Redstone Interface named <name> is linked to this machine` | This computer has no interface of that name. |

## If something goes wrong

> [!PROBLEM] SET REDSTONE needs the interface's name, in quotes when it has spaces
> Type the interface's name after REDSTONE.

> [!PROBLEM] SET REDSTONE needs IN, or OUT and a strength from 0 to 15
> Add IN, or OUT followed by a number.

> [!PROBLEM] not a redstone strength, which runs from 0 to 15: <text>
> Type a whole number from 0 to 15.

> [!PROBLEM] no Redstone Interface named <name> is linked to this machine
> Use the name of an interface that this computer is linked to.

> [!SEE] jsc:redstone_interfaces, jsc:set_bus
