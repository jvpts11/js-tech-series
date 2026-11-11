---
section: iql_saved_objects
order: 378
---
# Views

> [!DEFINE] view
> A saved statement with a name. Each time you type the name, the network runs the saved statement again.

A view is a statement that you save under a name. You write it once, with CREATE VIEW. Then each time you type the name, the network runs the saved statement again, so the answer is what it is now.

The saved statement is kept exactly as you typed it. It can be any statement, though it is usually a QUERY.

```iql views.iql
CREATE VIEW low_iron AS QUERY items WHERE item = iron_ingot AND qty < 100
```

1. Type CREATE VIEW, a name, AS and the statement, all on one line.
2. Press [[Enter]] to save the view.
3. Type QUERY and the name to run it, such as QUERY low_iron.

Saved statements are kept on the Mainframe. A name is unique for each kind of saved statement, and capitals do not matter.

Views need the [Midsoft IQL Server](jsc:midsoft_iql_server) on the Mainframe. Other engines refuse them.

To remove a view, type DROP VIEW and its name, such as DROP VIEW low_iron.

Table: The replies for views

| Reply | What it means |
| --- | --- |
| `view <name> dropped` | The view is gone. |
| `no view named <name>` | No view has that name. |
| `DROP VIEW needs a name` | Type the name after DROP VIEW. |

## If something goes wrong

> [!PROBLEM] CREATE expects VIEW, PROCEDURE, or JOB
> Type VIEW, PROCEDURE or JOB after CREATE.

> [!PROBLEM] CREATE VIEW needs a name and an AS body
> Type a name after VIEW, then AS and the statement. The same holds for PROCEDURE and JOB.

> [!PROBLEM] expected AS after the VIEW name
> Type the word AS after the name.

> [!PROBLEM] CREATE VIEW needs a body after AS
> Type the statement after AS.

> [!PROBLEM] a name is at most 64 characters long
> Shorten the name to 64 characters or fewer.

> [!PROBLEM] the network's engine keeps no views, procedures or jobs
> Use an engine that keeps them: the Midsoft IQL Server ([](jsc:midsoft_iql_server)).

> [!SEE] jsc:iql_procedures, jsc:iql_jobs, jsc:midsoft_iql_server
