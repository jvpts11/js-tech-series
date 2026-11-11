---
section: operations
order: 136
icon: jsc:mainframe
---
# Priorities

Every Operation has a priority, which decides which waiting Operation gets a free queue first. There are five levels, from the lowest to the highest. A new Operation starts at MEDIUM, unless you choose another level.

> [!DEFINE] priority
> The level, from LOW to HIGH, that decides which waiting Operation gets a free queue first.

When two Operations have the same level, the one that came first goes first.

Table: The five levels

| Level | Keyword in IQL | Short label |
| --- | --- | --- |
| `LOW` | `low` | LOW |
| `MEDIUM_LOW` | `medium_low` | MED- |
| `MEDIUM` | `medium` | MED |
| `MEDIUM_HIGH` | `medium_high` | MED+ |
| `HIGH` | `high` | HIGH |

Keywords are not case-sensitive, and you can write medium-low or medium-high with a hyphen. The keyword normal means MEDIUM. The short label is what dense lists show.

> [!DEFINE] aging
> The rise of a waiting Operation's level: one step for each period it waits for a queue.

An Operation that is ready but gets no free queue climbs one level for every 30 seconds it waits. By default, that is 600 ticks. It climbs no higher than HIGH, so low work is delayed, but it is never left behind for good. The mod's configuration sets this period, and a period of 0 switches aging off.

Aging does not change the level you chose. Each tick, the Mainframe works out the level it uses for that tick: the chosen level, plus what aging has added.

## Changing a priority

1. Open the Processes tab of the Network Manager, and click the row of the Operation.
2. Press > to raise its level one step, or < to lower it.

In IQL, end a statement with PRIORITY and a level, such as SELECT 64 iron_ingot PRIORITY HIGH. In the Network program, the popup of a request has a PRIORITY setting that raises or lowers the level one step at a time. A CRAFT takes its level from the statement or from its request ([](jsc:asking_for_a_craft)).

## If something goes wrong

> [!PROBLEM] An Operation is WAITING, and raising its level changes nothing.
> A waiting Operation waits for a hold on items, not for a queue. Wait until the hold ends, or UNLOCK the item ([](jsc:lock_unlock)).

> [!PROBLEM] The result says PRIORITY is not valid on a read.
> Take PRIORITY off the QUERY. It sets the level of an Operation, and a QUERY only reads ([](jsc:clauses_priority_if_with)).

> [!PROBLEM] The result says unknown priority level, and lists the five names.
> Write one of the five names from the table, such as HIGH.

> [!SEE] jsc:states_and_queues, jsc:iql
