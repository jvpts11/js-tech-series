---
section: iql_settings
order: 376
---
# SET INTERFACE, SET ROUTER, RENAME, PAUSE and RESUME

A Crafting Interface holds the patterns and asks the network for crafts. A Crafting Input Router sends the inputs of a pattern to a router that you choose. The router must sit on the same cable ([](jsc:crafting_interfaces)).

These statements change an interface or a router, or their names. When a name has spaces, you write it in quotes.

Table: The statements for interfaces and routers

| Statement | What it does |
| --- | --- |
| `SET INTERFACE 'x' EXCLUSIVE ON` | The interface crafts one recipe at a time. |
| `SET INTERFACE 'x' EXCLUSIVE OFF` | The interface may run several recipes. |
| `SET INTERFACE 'x' MAX JOBS 4` | At most 4 crafts at a time. 0 means as many as come. |
| `SET INTERFACE 'x' MAX JOBS AUTO` | The same as 0. |
| `SET INTERFACE 'x' ROUTE 'pattern' INPUT item TO ROUTER 'router'` | Sends that input of the pattern to the router you name. |
| `SET INTERFACE 'x' ROUTE 'pattern' INPUT item AUTO` | Sends that input without naming a router. |
| `SET ROUTER 'x' setting` | Applies any setting of SET BUS ([](jsc:set_bus)) to a Crafting Input Router. |
| `PAUSE INTERFACE 'x'` | Pauses the interface. |
| `RESUME INTERFACE 'x'` | Starts the paused interface again. |
| `RENAME INTERFACE 'x' TO 'new name'` | Gives the interface a new name. RENAME ROUTER does the same for a router. |

Table: The replies

| Reply | What it means |
| --- | --- |
| `<name> set` | The change is saved. |
| `<old> is now called <new>` | The rename worked. |
| `no Crafting Interface named <name> on this network` | No interface of that name is on the network. |
| `no Crafting Input Router named <name> on this network` | No router of that name is on the network. |
| `the interface <name> holds no pattern called <pattern>` | The interface has no pattern of that name. |
| `<input> is not an input of <pattern>` | Name one of the pattern's inputs. |
| `no router named <router> on the cable of the interface <name>` | The router is not on the cable of that interface. |

## If something goes wrong

> [!PROBLEM] SET INTERFACE needs the interface's name, in quotes when it has spaces
> Type the interface's name after INTERFACE.

> [!PROBLEM] SET ROUTER needs the router's name, in quotes when it has spaces
> Type the router's name after ROUTER.

> [!PROBLEM] SET INTERFACE needs a setting: EXCLUSIVE, MAX JOBS or ROUTE
> Add one of those three settings after the name.

> [!PROBLEM] an interface has no setting <word>
> Use EXCLUSIVE, MAX JOBS or ROUTE.

> [!PROBLEM] RENAME needs TO and the new name
> Write TO and the new name after the old name.

> [!PROBLEM] expected the pattern's name
> Type the pattern's name after ROUTE.

> [!PROBLEM] expected one of the pattern's inputs
> Type the item of the input after INPUT, or AUTO.

> [!PROBLEM] expected the router's name
> Type the router's name after TO ROUTER, or write AUTO.

> [!SEE] jsc:crafting_interfaces, jsc:pattern_encoders, jsc:set_bus
