---
section: operations
order: 138
icon: jsc:mainframe
---
# IQL, the network's language

Clicking in the Network program is fine for taking a stack of iron. But what if you want to ask which items the network holds more than 100 of, or to craft 64 torches from a script? That is what IQL is for: asking the network for anything, in words, at a command prompt, in a program or in a script.

IQL reads like the language of databases. A few lines show the idea:

```table
{"caption": "A few lines of IQL", "rows": [[{"words": "Take items"}, {"literal": "SELECT 64 iron_ingot"}], [{"words": "Craft"}, {"literal": "CRAFT 64 torch"}], [{"words": "Ask"}, {"literal": "QUERY items WHERE qty > 100"}], [{"words": "Sort and cut"}, {"literal": "QUERY items ORDER BY name LIMIT 2"}], [{"words": "Look at the disks"}, {"literal": "QUERY disks"}]]}
```

A statement starts with a verb. An action needs a quantity and an item, and it may add other clauses. A question starts with QUERY or SHOW. The quantity is a number, or ALL for everything the network holds of that item. Percentages such as 50% are not supported.

```table
{"caption": "The shape of an action", "rows": [[{"words": "Start"}, {"literal": "VERB [quantity] item"}], [{"words": "Then, in any order"}, {"literal": "[FROM place] [TO place] [WHERE condition] [IF condition] [ORDER BY field] [LIMIT n] [PRIORITY level]"}]]}
```

## Running a statement

1. Open the Command Prompt on a computer that is on the network.
2. Type iql, a space, and the statement, such as iql SELECT 64 Cobblestone.
3. Press [[Enter]].

The words operation, op and sql do the same as iql. Outside the network, the command refuses to run, and it says that the computer is not on a network.

The IQL Server Management Studio is the full console: you query the network, trace its work, look after its index and locks, and run its jobs from one place. It needs the Midsoft IQL Server, version 2000 or newer ([](jsc:studio_overview)).

A script is statements separated by semicolons. Two dashes start a comment that runs to the end of the line. A script can also have parameters, written like <name, type, default>, and the Studio asks you for their values when you run it.

> [!WARNING] DROP destroys items for good. DELETE with ALL sends out everything the network holds of an item. The Studio asks before it runs either one, unless you have turned that question off.

On an engine that keeps them, you can save a query as a view or a procedure. A job runs statements on a timer, or when a condition is met. A job needs the Automation Engine installed on the Mainframe ([](jsc:automation_engine)).

The book Learning IQL explains every verb, clause and error, each in its own entry ([](jsc:what_iql_is)).

## If something goes wrong

> [!PROBLEM] The network refuses a line, with a message such as unknown verb.
> Check the word that the message names. The Studio points at the place where IQL stopped reading ([](jsc:iql_errors)).

> [!PROBLEM] The result says DELETE exports out of the network and needs a TO destination.
> Add TO, and the name of a bus ([](jsc:buses)).

> [!PROBLEM] The result says MOVE is internal and needs both FROM and TO.
> Name the server to take from, and the server to put into ([](jsc:storage_servers)).

> [!PROBLEM] The result says nothing to select.
> The network holds none of that item. Check its name, or store some first ([](jsc:storage_servers)).

> [!PROBLEM] A view, a procedure or a job is refused, and the result says the network's engine keeps none of them.
> Use an engine that keeps them ([](jsc:engines)). A job also needs the Automation Engine ([](jsc:automation_engine)).

> [!PROBLEM] The result says the IF did not hold, so the action was not run.
> Check the condition. The IF reads how much of the item the network holds.

> [!SEE] jsc:what_iql_is, jsc:iql_errors
