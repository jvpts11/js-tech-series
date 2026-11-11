---
section: iql_recipe_list
order: 404
icon: jsc:mainframe
---
# A saved report

A report is a query you save under a name, so you can read the same numbers again later. In IQL a saved query is a view ([](jsc:iql_views)). Each time you run it, the view reads the network again, so the numbers are current.

Only the Midsoft IQL Server keeps views. On a network whose engine is Prophet YourIQL or NextgreIQL, CREATE VIEW is refused.

> [!DEFINE] Row
> One line of a result. In the items table, one line is one kind of item.

Table: What the recipe needs

| Need | What it takes |
| --- | --- |
| Era | Vintage at the prompt; Legacy and newer for the studio |
| Program | None at the prompt; the IQL Server Management Studio for a file |
| Engine | The Midsoft IQL Server running, which every Mainframe has |

1. Type the CREATE VIEW line at the prompt, with iql in front. The reply reads "view stock_report created".
2. Type iql QUERY stock_report to read the report.
3. To keep the results in a file, open the IQL Server Management Studio, run the query, and choose Results to File.

```table
{"caption": "Lines for a report", "rows": [[{"words": "Save it"}, {"literal": "CREATE VIEW stock_report AS QUERY items WHERE qty > 0 ORDER BY qty DESC LIMIT 20"}], [{"words": "Read it"}, {"literal": "QUERY stock_report"}], [{"words": "Drop it"}, {"literal": "DROP VIEW stock_report"}]]}
```

The view's name can be at most 64 characters long. Do not name a view after a table: QUERY items would then run your view, not the items table.

Results to File writes the grid of results as a comma-separated values (CSV) file, a plain text table that spreadsheets open.

Table: The tables you can read

| Table | Columns |
| --- | --- |
| `items` | `item, qty, server, tag, enchant` |
| `servers` | `name, used, free, era` |
| `disks` | `disk, server, tier, size, used` |
| `operations` | `id, verb, state, item, moved, requested, priority` |
| `recipes` | `recipe, where, machine, chance` |
| `computers` | `name, kind, era, state, os` |

The items table also reads name, count, amount, damaged and durability in a WHERE or an ORDER BY, though they do not show as columns. The name is the item's name as the game shows it.

The tag column shows one tag: the first c: tag of the item, in alphabetical order. An item with two c: tags shows only one, so a WHERE on tag can miss the other. An item with no c: tag shows its first tag.

A job can run a query too, but it throws the rows away, so a job gives you no report. Run the view yourself. A script file cannot hold a report either, because run refuses QUERY.

## If something goes wrong

> [!PROBLEM] The reply reads "no view named stock_report".
> The view was not saved, or it has another name. Type the CREATE VIEW line again.

> [!PROBLEM] The reply reads "IQL recursion too deep (a procedure or view referencing itself?)".
> The view calls itself. Remove the reference to the view's own name.

> [!PROBLEM] The reply reads "IQL stopped after 4096 statements for one request".
> The view makes too many statements for one request. Make it shorter, or take out a procedure.

> [!PROBLEM] The reply reads "a name is at most 64 characters long".
> Shorten the view's name.

> [!PROBLEM] The studio reads "There are no results to save."
> Run the query first, so there are results to save.

> [!PROBLEM] The reply reads "QUERY/COUNT are not supported by 'run', use 'operation' instead".
> Type the view's name at the prompt, or open the studio.

> [!SEE] jsc:iql_views, jsc:studio_overview, jsc:iql_limits, jsc:items_as_data
