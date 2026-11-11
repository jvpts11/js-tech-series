---
section: isms_studio
order: 396
---
# Templates and the IQL Reference

A template is a statement with blanks, which you fill in before it runs. The Template Explorer, in the View menu, lists eight of them. Click one, and you open it in a new query.

> [!DEFINE] Template
> A saved statement with blanks in it. The blanks are filled in before the statement runs.

Table: The eight templates

| Template | Statement it opens |
| --- | --- |
| Query a table | `QUERY <table, table, items> WHERE <column, column, qty> < <value, number, 64> ORDER BY <column, column, qty> DESC LIMIT <rows, number, 100>;` |
| Count an item | `COUNT <item, name, iron_ingot>;` |
| Craft an item | `CRAFT <quantity, number, 64> <item, name, iron_ingot>;` |
| Create a view | `CREATE VIEW <name, name, low_stock> AS QUERY items WHERE qty < <value, number, 64>;` |
| Create a procedure | `CREATE PROCEDURE <name, name, restock> AS { CRAFT <quantity, number, 64> <item, name, torch>; };` |
| Create a job | `CREATE JOB <name, name, nightly_vacuum> AS VACUUM EVERY <interval, interval, 20m>;` |
| Hold an item | `LOCK <quantity, number, 64> <item, name, diamond>;` |
| Update an item | `UPDATE <quantity, number, 1> <item, name, iron_sword> SET REPAIR;` |

A blank has three parts in its angle brackets: a name, a type and a default value. For example, <quantity, number, 64> is a blank named quantity, of the type number, whose value starts as 64.

Press [[Ctrl]]+[[Shift]]+[[M]] to fill in the blanks of the current query. The dialog Specify Values for Template Parameters asks for each value, and shows each blank as its name and type.

## The IQL Reference

Press [[F1]] to open the IQL Reference. It lists each statement with what it does. The table below gives the same list, with the meaning the studio gives each line.

Table: The IQL Reference

| Syntax | What it does |
| --- | --- |
| `QUERY table [WHERE ...] [ORDER BY ...] [LIMIT n]` | Reads the rows of a table. |
| `COUNT item` | Counts an item the network holds. |
| `SELECT n item` | Brings an item to this computer. |
| `INSERT n item` | Stores an item from this computer. |
| `DELETE n item TO place` | Sends an item out of the network. |
| `MOVE n item FROM server TO server` | Moves an item between servers. |
| `DROP n item` | Destroys an item, for good. |
| `CRAFT n item` | Makes an item from its recipes. |
| `UPDATE n item SET action` | Changes an item with a personal-use card. |
| `LOCK n item / UNLOCK item` | Holds an item, or lets it go. |
| `ANALYZE / REINDEX / VACUUM` | Looks after the index. |
| `CREATE VIEW name AS query` | Saves a query under a name. |
| `CREATE PROCEDURE name AS { ... }` | Saves statements to run together. |
| `CREATE JOB name AS ... EVERY time` | Runs statements on a timer or a condition. |
| `EXEC name` | Runs a saved procedure. |
| `WHERE / ORDER BY / LIMIT` | WHERE keeps the rows that match, ORDER BY sorts them, LIMIT stops after so many. |
| `; --` | A semicolon ends a statement; two dashes start a comment. |

## If something goes wrong

> [!PROBLEM] [[Ctrl]]+[[Shift]]+[[M]] says "This query has no template parameters."
> Open a template from the Template Explorer, or write the statement by hand.

> [!SEE] jsc:first_queries, jsc:query_and_select, jsc:iql_count, jsc:insert_delete_move_drop, jsc:iql_craft, jsc:update_cards, jsc:lock_unlock, jsc:iql_views, jsc:iql_procedures, jsc:iql_jobs
