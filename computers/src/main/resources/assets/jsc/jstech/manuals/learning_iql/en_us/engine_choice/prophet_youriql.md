---
section: engine_choice
order: 388
icon: jsc:mainframe
---
# Prophet YourIQL: keeping a level

Prophet YourIQL is the engine that keeps a level for you. Instead of asking for a craft each time, you write KEEP steel_ingot >= 10000 once. The engine then works out what it must make to keep that many steel ingots on the network.

> [!DEFINE] State
> A level the engine is told to keep, written as one KEEP statement.

> [!DEFINE] Band
> The range between a lower and an upper level, which a state keeps.

> [!DEFINE] Watch
> A statement that waits for a condition, then runs one action when the condition comes true.

Table: Prophet YourIQL versions by age

| Age of the Mainframe | Prophet YourIQL version |
| --- | --- |
| Legacy | 3.23 |
| Transition | 5.0 |
| Standard | 5.6 |
| Advanced | 8.0 |

Its own tool is the Prophet Reactive Console ([](jsc:prophet_console)), which needs YourIQL 3.23 or newer.

## The statements

Table: The statements of Prophet YourIQL

| Statement | What it does |
| --- | --- |
| `KEEP item >= n` | Keeps at least n of the item. |
| `KEEP item BETWEEN a AND b` | Keeps the item within a band, from a to b. |
| `WATCH` | Runs an action once when a condition comes true. |
| `FORGET item` | Stops keeping the item. |
| `FORGET WATCH n` | Removes the watch with that number. |
| `SHOW STATES` | Lists the kept states and the watches in a table. |

Any other statement goes to the core language unchanged.

## Keeping a level

1. Type KEEP, the item's short name, >= and a number.
2. Read the answer, which names the item and its level.

```iql keep_steel.iql
KEEP steel_ingot >= 10000
```

KEEP with >= takes exactly four words, and >= must stand as a word of its own, with spaces around it. KEEP with BETWEEN takes exactly six words, and its upper number must not be below its lower number.

Numbers are whole numbers written in digits, with no minus sign. The answer says that the item is "kept at" a level "or more", or "kept between" two levels.

Keeping the same item again replaces its band.

## The band and Over

KEEP BETWEEN makes the engine craft when the level falls below its band. The upper number only shows where the level stops being right. Nothing is thrown away to bring a level down, so going over the band is never corrected by removing items.

## How the engine keeps a level

Each time it looks, the engine counts what the network holds and what is already on its way. Work on its way is an Operation that has started and not yet finished.

When the level is below its band, the engine asks the network to craft what is missing. It asks for at most one batch per craft, and the batch size is a setting. The request goes through the network's crafting door, under the name Prophet YourIQL ([](jsc:how_autocrafting_works)).

If nothing on the network can make the item, the state shows Cannot hold. The engine tries again every 600 ticks, which is 30 seconds.

## The settings

The Prophet Reactive Console sets three things for this engine.

Table: The settings of Prophet YourIQL

| Setting | Choices | Default |
| --- | --- | --- |
| How often it looks | Every 1, 2, 5 or 10 seconds | 1 second |
| Largest batch per craft | 64, 256, 1,024 or 4,096 items | 1,024 items |
| Reacting | On or off | On |

A value that is not on the list falls back to its default. The engine reacts only while it runs, so a stopped or replaced engine stops reacting.

The states and the watches are saved with the Mainframe. The engine keeps 48 level samples for each state's graph, the 32 latest reactions, and up to 4 Operations for each state in the console.

## If something goes wrong

> [!PROBLEM] The answer is "syntax: KEEP item >= n, or KEEP item BETWEEN a AND b".
> Write KEEP with >= and a number, or with BETWEEN and its two numbers.

> [!PROBLEM] The answer is "no item is called" and the name you typed.
> Check the spelling of the item's short name.

> [!PROBLEM] A state shows Cannot hold.
> Nothing on the network can make the item. Look at the state in SHOW STATES, and check that the network can make it.

> [!PROBLEM] A state shows Over.
> Nothing is wrong. The engine does not remove items to bring a level down.

> [!SEE] jsc:prophet_watch, jsc:prophet_console
