---
section: iql_verbs
order: 374
---
# PRIORITY, IF, OFFER and WITH

The words after the item are clauses. Each verb accepts some of them. A clause that the verb does not accept is refused with a message, such as "FROM is not valid on a read". The table shows what each verb accepts.

Table: Clauses each verb accepts

| Verb | Clauses it accepts |
| --- | --- |
| `QUERY` | WHERE, ORDER BY and LIMIT. FROM, TO, IF and PRIORITY are refused. |
| `SELECT` | FROM (optional), WHERE, IF, ORDER BY, LIMIT and PRIORITY. TO is refused. |
| `INSERT` | FROM (optional, a bus name), WHERE, IF, ORDER BY, LIMIT and PRIORITY. TO is refused. |
| `DELETE` | TO (required), WHERE, IF, ORDER BY, LIMIT and PRIORITY. FROM is read, but has no effect. |
| `MOVE` | FROM and TO (both required), WHERE, IF, ORDER BY, LIMIT and PRIORITY. |
| `DROP` | WHERE, IF, ORDER BY, LIMIT and PRIORITY, with FROM ignored and TO refused. |
| `CRAFT` | WHERE, IF, ORDER BY, LIMIT and PRIORITY. FROM and TO are read, but have no effect. |
| `COUNT` | WHERE, ORDER BY and LIMIT. IF, FROM, TO and PRIORITY are read, but have no effect. |
| `LOCK, UNLOCK` | WHERE, IF, ORDER BY, LIMIT and PRIORITY, which has no effect. |
| `UPDATE` | SET (required), FROM (optional, a server), WHERE, IF, ORDER BY, LIMIT and PRIORITY. TO is refused. |
| `ANALYZE, VACUUM, REINDEX` | No clause. One object word, which is ignored. |

## PRIORITY

PRIORITY sets the level of each Operation a statement queues ([](jsc:priorities)). The default is MEDIUM, and you write the level after the word, such as PRIORITY HIGH.

Table: The five levels

| Level | Short label in the Task Manager |
| --- | --- |
| `LOW` | `LOW` |
| `MEDIUM_LOW` | `MED-` |
| `MEDIUM` | MED, the default |
| `MEDIUM_HIGH` | `MED+` |
| `HIGH` | `HIGH` |

NORMAL means MEDIUM. A hyphen reads as an underscore, and capitals do not matter, so medium-high is MEDIUM_HIGH.

## IF

IF runs the statement only when a condition holds. The condition uses the words of a WHERE condition ([](jsc:query_and_select)). The network checks it against its stock of the named item before the statement runs.

Table: The fields an IF can check

| Field | What the check compares |
| --- | --- |
| `qty, count, amount` | The total the network holds of the item, every copy counted. |
| `item` | The item's id, or \* for any item. |
| `name` | The display name, or the item as you typed it when the network holds none. |
| Any other field | Never true, so the statement does not run. |

When the condition does not hold, the statement does not run. The reply is not an error: "the IF did not hold, so the SELECT was not run", with the verb you typed. A file goes on to its next line.

## WITH

WITH gives UPDATE its second item: the material for REPAIR, or the item COMBINE adds. It comes after the action word, and no other statement takes it.

## OFFER

OFFER takes one of the three enchantment offers, after ENCHANT. You type 1, 2 or 3 after the word. ([](jsc:update_cards)) shows what each offer means.

## If something goes wrong

> [!PROBLEM] <ACTION> takes no WITH
> Remove WITH. SMELT, ENCHANT and NAME take none.

> [!PROBLEM] COMBINE needs WITH and the item it adds
> Add WITH and the second item.

> [!PROBLEM] unexpected token: WITH
> Put WITH after the action word of an UPDATE.

> [!PROBLEM] an offer is 1, 2 or 3, not <n>
> Type 1, 2 or 3 after OFFER.

> [!PROBLEM] an offer after OFFER (1, 2 or 3)
> You left the number out. Type it right after the word OFFER.

> [!PROBLEM] unknown priority level: <word> (expected LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH or HIGH)
> Type one of the five level words.

> [!PROBLEM] IF is not valid on a read
> A QUERY cannot have IF. Use WHERE to pick the rows instead.

> [!SEE] jsc:priorities, jsc:update_cards, jsc:iql_count
