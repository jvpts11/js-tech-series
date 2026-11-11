---
section: engine_programs
order: 399
---
# Prophet Reactive Console

The Prophet Reactive Console keeps the network's stock where you want it. You tell it which items to keep. When a level falls, it asks the network for what is missing, on its own.

The console needs the YourIQL engine, version 3.23, running on the Mainframe. Its own statements are explained in [the YourIQL engine](jsc:prophet_youriql).

> [!DEFINE] State
> One item the network is told to keep. In the States list, each state is one KEEP line.

> [!DEFINE] Band
> The range that a state is kept inside: at least a number, or between two numbers.

> [!DEFINE] Level
> What the network counts for an item: the amount held, plus the amount on its way, plus the amount arriving.

## The prompt

Type a YourIQL statement at the prompt, then press Check or Apply. Check reads the statement without changing anything, and answers "The statement reads well." when it is right. Apply runs the statement.

A statement that does not start with KEEP, WATCH, FORGET or SHOW STATES goes to the normal IQL engine, explained in [the engines](jsc:iql_engines_overview).

## The tabs

Table: The four tabs

| Tab | What it shows |
| --- | --- |
| States | The states the network is held to, one line each. |
| Subscriptions | The watches, each with its number and status. |
| Reactions | What the engine did lately. |
| Settings | How often the engine looks, how much it asks for at once, and whether it reacts. |

## The statements

Table: The YourIQL statements

| Statement | Form | What it does |
| --- | --- | --- |
| KEEP | `KEEP <item> >= <n>` | Keeps at least <n> of the item, counting what is on its way. |
| KEEP with a band | `KEEP <item> BETWEEN <a> AND <b>` | Keeps the level between <a> and <b>. |
| WATCH | `WATCH <item> < <n> DO <statement>` | Runs the statement once, when the level crosses the line. The comparisons are <, <=, > and >=. |
| WATCH with CRAFT | `WATCH <item> < <n> DO CRAFT <item> TO <n>` | Makes the item up to level <n>, when the watch fires. |
| FORGET | `FORGET <item>` | Stops keeping the item. |
| FORGET WATCH | `FORGET WATCH <n>` | Removes watch number <n>. |
| SHOW STATES | `SHOW STATES` | Lists the states and the watches. |

A number is a whole number, zero or more. An upper end in a band only says where the level stops being where it should be: nothing is thrown away to bring it down. The recipe [keep a stock of an item](jsc:recipe_keep_stock) shows a KEEP line at work.

## The States tab

The States tab lists each state, with where it stands and what is being done for it. Under the list, the graph of the selected state shows its level.

Table: The columns of the States tab

| Column | What it shows |
| --- | --- |
| DESIRED STATE | The band, or the minimum, that you asked for. |
| STATE | Where the state stands, as one of the words in the next table. |
| NOW | The level, against its band. |
| WORK | The Operations on its way, as "<n> on the way", or "no operations". |
| LAST REACTION | The last thing done for the state. |

Table: The states

| Status | Meaning |
| --- | --- |
| New | Not looked at yet. |
| Holding | In its band, with nothing on its way. |
| Working | Work is on its way to bring it into its band, or to keep it there. |
| Cannot hold | Below its band, and nothing could be set going. |
| Over | Above its band. Nothing is thrown away to bring it down. |
| Armed | A watch, waiting for its condition. |
| Fired | A watch that fired, and waits for its condition to stop. |

When the level is below its band, the engine asks the network for the difference as a craft. If nothing could be set going, the state reads Cannot hold, and the engine tries again after 600 ticks (30 seconds).

A state above its band, with nothing on its way, reads Over. With work on its way, it reads Working.

## The Subscriptions tab

The Subscriptions tab lists the watches. Each watch shows its number, its status, Armed or Fired, and how many times it fired, as "fired <n> times". Press Forget to remove a watch. The number is the one that FORGET WATCH uses.

## The Reactions tab

The Reactions tab lists what the engine did lately, with how long ago, as "<n> s ago" or "<n> min ago". It keeps at most 32 reactions.

Table: The reactions

| Reaction | What happened |
| --- | --- |
| asked for <n> <item>, Operation <id> | The engine asked the network for a craft, which is now an Operation. |
| nothing on the network makes <item> | The engine wanted an item that no recipe on the network makes. |
| watch <n> fired: <statement> | A watch fired, and its statement ran. |
| watch <n> fired, and was refused: <reason> | A watch fired, but its statement was refused. |
| watch <n> fired with nothing to make | A watch fired, and there was nothing to make. |

## The Settings tab

The Settings tab sets how often the engine looks at the network, how much it asks for at once, and whether it reacts at all.

Table: The settings

| Setting | Choices | Default |
| --- | --- | --- |
| Look at the network every <n> s | 1, 2, 5 or 10 seconds | 1 second (20 ticks) |
| Ask for at most, in one craft | 64, 256, 1,024 or 4,096 items | 1,024 items |
| React to changes | On or Paused | On |

When React to changes is Paused, the engine makes no reactions at all. The states are still shown.

## If something goes wrong

> [!PROBLEM] The console refuses to open, and says that no compatible engine was found on this network.
> Run the YourIQL engine on the Mainframe ([](jsc:engine_services)).

> [!PROBLEM] The statement is refused: "syntax: KEEP item >= n, or KEEP item BETWEEN a AND b".
> Write a KEEP line in one of these two forms.

> [!PROBLEM] The statement is refused: "syntax: WATCH item < n DO statement".
> Write the comparison as one of <, <=, > or >=, and put DO before the statement.

> [!PROBLEM] The statement is refused: "syntax: FORGET item, or FORGET WATCH n".
> Write FORGET with an item, or FORGET WATCH with a number.

> [!PROBLEM] The statement is refused: "no item is called <name>".
> Check the spelling of the item's name.

> [!PROBLEM] FORGET is refused: "<name> is not kept".
> Forget only an item that a KEEP line keeps. SHOW STATES lists them.

> [!PROBLEM] FORGET WATCH is refused: "no watch is numbered <n>".
> Write SHOW STATES to see the number of each watch.

> [!PROBLEM] The Reactions tab says "watch <n> fired, and was refused: <reason>".
> Forget that watch with FORGET WATCH, then set it again with a statement that works.

> [!PROBLEM] A state reads Cannot hold.
> Wait: the engine tries again after 600 ticks (30 seconds). Check that the network holds the items the craft needs.

> [!PROBLEM] The Reactions tab stays empty, and no state reacts.
> Open the Settings tab and set "React to changes" to On.

> [!SEE] jsc:prophet_youriql, jsc:prophet_watch, jsc:recipe_keep_stock
