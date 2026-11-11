---
section: iql_recipe_list
order: 400
icon: jsc:mainframe
---
# Keep a stock of an item

A stock is how many of an item the network holds. Often you want a number to stay where it is: 512 iron ingots ready for the next build, and never fewer. IQL has three ways to ask for that, and they work differently. Choose the one that fits what you need.

> [!DEFINE] Stock
> How many of an item the whole network holds, counted over all its servers.

Table: The ways to keep a stock

| Way | Era | What it needs |
| --- | --- | --- |
| KEEP, a level that Prophet YourIQL holds | Legacy and newer | Prophet YourIQL running on the Mainframe |
| A job that restocks below a line | Legacy and newer | The Midsoft IQL Server running, and the Automation Engine installed |
| An IF on one action, once | Vintage and newer | An engine that runs actions, such as the Midsoft IQL Server |

KEEP belongs to Prophet YourIQL ([](jsc:prophet_youriql)). You name the item and the level, and the engine works out the crafts that reach the level and keep it there.

## KEEP: name the level, and let the engine hold it

The engine counts what is already on its way: the items being crafted or moved toward the stock. So it orders only what is still missing.

1. Install Prophet YourIQL on the Mainframe, and make sure the Mainframe is on.
2. Open the Prophet Reactive Console on a computer of the network.
3. Type the KEEP line in the console and press [[Enter]].
4. Type SHOW STATES to see the item and its status.

Table: Lines for KEEP

| Line | What it asks for |
| --- | --- |
| `KEEP iron_ingot >= 512` | At least 512 iron ingots, crafted as needed. |
| `KEEP iron_ingot BETWEEN 256 AND 512` | Between 256 and 512 iron ingots. |

The spaces around >= are required, and so are the words BETWEEN and AND. A name with no colon means minecraft:, so iron_ingot is the vanilla iron ingot. A mod's item needs its full name, with the mod's name before the colon.

When the level is kept, the console confirms it with a line that ends in "kept at 512 or more".

The engine looks at the network in ticks. One tick is one step of the game, and 20 ticks make one second.

> [!DEFINE] Tick
> One step of the game: 20 of them make one second.

Table: Settings of the engine

| Setting | Choices | Default |
| --- | --- | --- |
| Look at the network every | 20, 40, 100 or 200 ticks: 1, 2, 5 or 10 seconds | 20 ticks, 1 second |
| Ask for at most, in one craft | 64, 256, 1,024 or 4,096 items | 1,024 items |

Two settings change how Prophet works: the look interval and the batch per craft. Prophet reacts only while the Mainframe is on, and only when reacting is turned on.

A watch, written WATCH item < n DO statement, runs that statement once when a level crosses the line you set. Inside a watch, CRAFT item TO n makes the item up to a level.

Type SHOW STATES to see each state's status. Cannot hold means the network cannot reach the level right now.

## A job: restock when the stock falls below a line

A job is a saved statement that the network runs by itself ([](jsc:iql_jobs)). The Automation Engine keeps the jobs and fires them on time, and it does so only while the Mainframe is on.

> [!DEFINE] Automation Engine
> The Mainframe's service that keeps the jobs and fires them on time.

A job with a WHEN trigger fires on the rising edge of its condition: the moment the condition turns from false to true. It fires once for that moment, and not again while the condition stays true.

> [!DEFINE] Rising edge
> The moment a condition turns from false to true.

The job crafts an item from a pattern, the recipe the network holds for that item.

> [!DEFINE] Pattern
> The recipe the network holds for an item, which tells it what to craft that item from.

1. Install the Automation Engine on the Mainframe ([](jsc:automation_engine)).
2. Check that the Mainframe is on and that the Midsoft IQL Server is running.
3. Make sure the network holds a pattern for the item ([](jsc:patterns)).
4. Type the job at the prompt, with iql in front ([](jsc:where_to_type_iql)).

```table
{"caption": "A line for a job", "rows": [[{"words": "Restock 64 when below 512"}, {"literal": "CREATE JOB restock_iron AS CRAFT 64 iron_ingot WHEN qty(iron_ingot) < 512"}]]}
```

The reply reads "job restock_iron created". The job checks the stock about every half second. If the stock is already below 512 at its first check, the job fires at once.

The job looks only at the stock on the network, not at what is on its way. It fires again only after the stock has reached 512 or more and fallen below it once more.

The Automation Manager writes the same job for you, without any IQL. Its form is called Restock below: you give it an item and an amount, and it creates a job that crafts that amount when the stock falls below the same amount ([](jsc:automation_manager)).

## An IF guard: one craft, when the stock is low

An IF clause on one action runs that action once, and only if its condition holds. It does not come back by itself, so use it for one order, not for a standing rule.

```table
{"caption": "A line with an IF", "rows": [[{"words": "Craft 64 only when the stock is below 100"}, {"literal": "CRAFT 64 iron_ingot IF qty < 100"}]]}
```

The IF reads the network's whole stock of the item the action names. When the condition does not hold, the reply reads "the IF did not hold, so the CRAFT was not run". That is not an error: the line went through and did nothing.

## If something goes wrong

> [!PROBLEM] The reply reads "syntax: KEEP item >= n, or KEEP item BETWEEN a AND b".
> Write the spaces around >= and the words BETWEEN and AND, and give a level with each.

> [!PROBLEM] The reply reads "no item is called steel_ingt".
> Check the item's id. A mod's item needs its mod's name before the colon.

> [!PROBLEM] The reply reads "jobs are kept by the Automation Engine; install it on the Mainframe first".
> Install the Automation Engine on the Mainframe ([](jsc:automation_engine)).

> [!PROBLEM] The reply reads "the network's engine keeps no views, procedures or jobs".
> Prophet YourIQL and NextgreIQL keep no jobs. Use the Midsoft IQL Server for a job ([](jsc:engines)).

> [!PROBLEM] SHOW STATES says Cannot hold.
> The network cannot reach the level. Give the network a pattern for the item and the materials that pattern uses.

> [!PROBLEM] Nothing fires while the Mainframe is off.
> Turn the Mainframe on. Jobs and Prophet YourIQL wait for it.

> [!SEE] jsc:iql_jobs, jsc:prophet_youriql, jsc:automation_manager, jsc:engines
