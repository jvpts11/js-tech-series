---
section: engine_choice
order: 389
icon: jsc:mainframe
---
# Prophet YourIQL: watches, forgetting and SHOW STATES

A watch runs an action when a condition comes true. You write the condition as an item, a comparison and a number. After DO you write the action, as you would write any statement.

1. Type WATCH, the item, the comparison and the number.
2. Type DO, then the action.

```iql watch_redstone.iql
WATCH redstone < 500 DO CRAFT redstone TO 1000
```

Table: The comparisons

| Comparison | Means |
| --- | --- |
| `<` | below |
| `<=` | at most |
| `>` | above |
| `>=` | at least |

The first watch you set is number 1, and the next ones count up from it. The answer says watch 1 set, and then writes the watch back in full, so you can check it.

A watch fires once, when its condition comes true. It is armed again only after the condition has stopped being true.

## What a watch does

Table: The actions of a watch

| Action | What happens |
| --- | --- |
| `CRAFT item TO n` | Makes up to n of the item, counting what the network holds, batch by batch. |
| Any other statement | Runs as a statement, and its answer is shown, up to 64 rows. |

## Forgetting

Table: Forgetting an item or a watch

| Statement | What happens |
| --- | --- |
| `FORGET item` | The item is no longer kept. |
| `FORGET WATCH n` | The watch with that number is let go. |

FORGET takes the name you wrote for the item. Forgetting an item also clears its record of recent Operations.

## SHOW STATES

SHOW STATES lists what the engine keeps and watches. Type it as two words, in any capitals. The answer counts the states and the watches, and then shows a table.

Table: The table of SHOW STATES

| Column | What it shows |
| --- | --- |
| state | The state as you wrote it, or the WATCH statement for a watch. |
| status | The status word from the table below. |
| held | What the network holds of the item. |
| coming | The Operations on their way for the item. |
| last | The last thing the engine did for it; left empty on a watch row. |

## The status words

Table: The status words

| Status | What it means |
| --- | --- |
| New | Not looked at yet. |
| Holding | In its band, with nothing on its way. |
| Working | Work is on its way to bring it into its band or keep it there. |
| Cannot hold | Below its band, and nothing could be set going. |
| Over | Above its band; nothing is thrown away to bring it down. |
| Armed | A watch waiting for its condition. |
| Fired | A watch whose condition came true. |

Holding is the good state. Cannot hold and Over need your attention. The others wait, or are in progress.

The engine does not react to watches while it is stopped, so no watch fires then.

## If something goes wrong

> [!PROBLEM] The answer is "syntax: WATCH item < n DO statement (the comparison one of <, <=, >, >=)".
> Write WATCH, the item, one comparison, a number, DO, and the action.

> [!PROBLEM] The answer says the watch fired, and was refused, with a reason after the colon.
> Read the reason, fix the statement, let the watch go with FORGET WATCH, and set it again.

> [!PROBLEM] The answer says the watch fired with nothing to make.
> The network already holds the target, so nothing is wrong.

> [!PROBLEM] FORGET says the item is not kept.
> Check the item's name in SHOW STATES.

> [!PROBLEM] FORGET WATCH says no watch is numbered that number.
> Check the number in SHOW STATES.

> [!SEE] jsc:prophet_youriql, jsc:prophet_console, jsc:engine_problems
