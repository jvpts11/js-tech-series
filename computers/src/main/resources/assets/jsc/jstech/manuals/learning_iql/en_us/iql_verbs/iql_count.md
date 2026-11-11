---
section: iql_verbs
order: 367
---
# COUNT

COUNT is written like a query, but it does not add up the quantities. It reads the same rows as QUERY, and the reply tells you how many rows there are.

```iql count.iql
COUNT items WHERE qty > 100
```

So the reply is a row count, such as "12 rows". To see each row's quantity, read the qty column of a QUERY items, or open the table view in the Management Studio ([](jsc:studio_queries)).

COUNT reads an IF clause, but it ignores it. Your rows are listed whatever the IF says.

## If something goes wrong

> [!PROBLEM] expected an item
> COUNT needs an object or an item after it, such as COUNT items.

> [!PROBLEM] <file>: QUERY/COUNT are not supported by 'run', use 'operation' instead
> A file that runs at the prompt refuses QUERY and COUNT. Type the read at the prompt, one statement at a time.

> [!SEE] jsc:query_and_select
