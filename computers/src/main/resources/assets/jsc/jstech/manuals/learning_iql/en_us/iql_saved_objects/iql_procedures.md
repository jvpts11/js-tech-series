---
section: iql_saved_objects
order: 379
---
# Procedures and EXEC

> [!DEFINE] procedure
> A list of statements saved under one name. EXEC runs the statements in order.

A procedure is a list of statements saved under one name. When you run it with EXEC, the statements run one after another, so one name does several things for you.

```iql restock.iql
CREATE PROCEDURE restock AS { SELECT 64 iron_ingot; SELECT 64 gold_ingot }
```

1. Type CREATE PROCEDURE, a name, AS, and the statements in braces, with a semicolon after each statement but the last.
2. Press [[Enter]] to save the procedure.
3. Type EXEC and the name to run it, such as EXEC restock.

A procedure can also hold a single statement, without braces. PROC means the same as PROCEDURE, and CALL means the same as EXEC.

The statements run in order. If one fails, the procedure stops there, and the statements before it have already run. Procedures need the same engine as views ([](jsc:iql_views)).

Table: The replies for procedures

| Reply | What it means |
| --- | --- |
| `procedure <name> ran <n> statements` | Every statement ran, and the reply counts them. |
| `procedure <name> stopped at statement <n>: <error>` | The statement with that number failed. The message after it says why. |
| `no procedure named <name>` | No procedure has that name. |
| `procedure <name> dropped` | The procedure is gone. |

A procedure can run another procedure or a view, up to 32 levels deep. One request may run at most 4,096 statements. Past either limit, the network stops and shows the message.

## If something goes wrong

> [!PROBLEM] CREATE PROCEDURE needs a name and an AS body
> Type a name after PROCEDURE, then AS and the statements.

> [!PROBLEM] procedure <name> stopped at statement <n>: <error>
> Fix the statement that failed, then run EXEC again.

> [!PROBLEM] IQL recursion too deep (a procedure or view referencing itself?)
> A procedure runs itself, directly or through another. Remove the loop.

> [!PROBLEM] IQL stopped after 4096 statements for one request (procedures running others too many times?)
> Give the procedures fewer statements to run, or split the work into several requests.

> [!PROBLEM] EXEC needs a procedure name
> Type the name after EXEC.

> [!PROBLEM] EXEC takes a single procedure name
> Run one procedure at a time, with one EXEC for each name.

> [!PROBLEM] no procedure named <name>
> Check the name, or create the procedure first.

> [!PROBLEM] DROP PROCEDURE needs a name
> Type the name after DROP PROCEDURE.

> [!SEE] jsc:iql_views, jsc:iql_jobs, jsc:midsoft_iql_server
