---
section: iql_limits_and_errors
order: 381
---
# Limits

The network sets limits on what one statement can take. Some limits cut a result short.

Others refuse the statement, or stop it part way, and show a message.

The table gives each limit and what happens when you pass it.

Table: The limits of IQL

| Limit | Value | What happens when you pass it |
| --- | --- | --- |
| Rows that a QUERY reads, by default | 4,096 rows | A LIMIT below that number cuts the rows shorter. |
| Item types one \* statement covers | 256 item types | A LIMIT above that is capped, and the reply says how many were shown. |
| Levels of NOT and brackets in a condition | 64 levels | The statement is refused. |
| Comparisons in one condition | 256 comparisons | The statement is refused. |
| Length of the name of a saved statement | 64 characters | The statement is refused. |
| Length of the new name an anvil gives | 50 characters | The statement is refused. |
| Enchantment offers, numbered | 1 to 3 | Any other number is refused. |
| Procedures that run procedures | 32 levels | The network stops with a recursion message. |
| Statements in one request | 4,096 statements | The network stops and says how many it ran. |
| Rows that a job's query reads | 64 rows | The job reads at most 64 rows. |
| How often the network checks the jobs | every 10 ticks | An interval is exact to about 10 ticks. |
| Redstone strength | 0 to 15 | Any other number is refused. |
| Hour of the day in SET BUS | 0 to 23 | Any other hour is refused. |
| Quantity of an item | A whole number of zero or more, or ALL | A percentage or a negative number is refused. For an action, no number and ALL do the same. |
| Row count after LIMIT | A whole number | A percentage is refused. A LIMIT of 0 means no limit. |

Some limits protect the network, not your statement. If a QUERY reaches 4,096 rows, use a WHERE to narrow the rows you read, so the rows you need are not cut short.

> [!SEE] jsc:iql_errors, jsc:query_and_select
