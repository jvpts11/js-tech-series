---
section: iql_verbs
order: 366
---
# QUERY and SELECT

QUERY reads the network and shows you what it holds. It moves nothing. SELECT is an Operation that brings items out of the network into this computer's own storage, its [local storage](jsc:local_storage). Both take WHERE, ORDER BY and LIMIT, which choose the rows you work on.

## QUERY

Name the object after QUERY: the table you want to read. You can also write an item, or a star for every item.

Table: Objects you can query

| Object | What the rows are |
| --- | --- |
| `items, or *` | Every item the network holds. |
| `servers` | Every server on the network. |
| `operations` | The Operations that run or wait. |
| `computers` | The computers on the network. |
| `recipes` | The recipes the network knows. |
| `disks` | The disks of the network. |
| `events` | The world events the network sees: under way where it stands, or coming when a machine on it is the event's instrument. |
| `power` | Every server of a rack with a PDU: the rack unit it tops, what it draws in watts, and whether its bay is on. |

Machines of other mods on the network have tables of their own. Name one by its id, such as QUERY mymod:reactors, or by the part after the colon when no other table has it. Each row is one machine, with its columns, their units in the heading, and at, where it stands.

A name no table has is refused: "No table is named <name>". A name two tables share asks for the id. After a QUERY, the reply counts the rows, such as "12 rows" or "1 row". To read the rows themselves, open the table view in the Management Studio ([](jsc:studio_queries)).

```iql stock.iql
QUERY items WHERE qty > 100 ORDER BY qty DESC LIMIT 5
```

## SELECT

SELECT takes items out of the network. You type a quantity, the item and, if you want, a server. A quantity of ALL, or no quantity at all, asks for everything the network can give. SELECT 64 iron_ingot and SELECT ALL iron_ingot do the same thing.

1. Type SELECT, a quantity and the item, such as SELECT 64 iron_ingot.
2. To take from one server only, add FROM and the server's name as one word.
3. Press [[Enter]], then read the reply.

Table: The replies of SELECT

| Reply | What it means |
| --- | --- |
| `SELECT queued: <quantity> <item> -> local storage` | The items are on their way to this computer. |
| `SELECT queued: <quantity> <item> from <server> -> local storage` | The same, taken from that one server. |
| `nothing to select` | You typed SELECT \*, and the network holds no item. |
| `no server named '<server>'` | No server has that name. Check its spelling. |

A star asks for every item type at once: SELECT \* queues one move for each item type the network holds, up to 256 types. The reply then says "12 item types" or "1 item type".

## WHERE: choosing rows

WHERE keeps only the rows that match a condition. A condition compares a field with a value, with an operator between them. qty > 100 keeps the rows whose quantity is over 100.

```table
{"caption": "Operators", "heads": ["Operator", "What it tests"], "rows": [[{"literal": "=  or  =="}, {"words": "Equal. Numbers compare as numbers; words compare without regard to capitals."}], [{"literal": "!="}, {"words": "Not equal."}], [{"literal": "<  >  <=  >="}, {"words": "Less than, more than, at most, at least."}], [{"literal": "contains"}, {"words": "The text holds the word, without regard to capitals."}], [{"literal": "has"}, {"words": "The same test as contains."}], [{"literal": "like"}, {"words": "A pattern, where * stands for any run of characters."}]]}
```

Combine conditions with AND and OR, reverse one with NOT, and use brackets to group them. NOT binds tightest, then AND, then OR. A condition can have at most 64 levels of NOT and brackets, and at most 256 comparisons. A condition ends at the first word it cannot use, such as IF, ORDER or LIMIT.

```iql rare.iql
QUERY items WHERE name contains "rare" AND NOT damaged = true
```

## ORDER BY and LIMIT

ORDER BY sorts the rows by one field: smallest first, or A to Z for words. ASC is the default, and DESC sorts the other way. LIMIT keeps only the first rows. It takes a row count, not a percentage.

Table: Sorting a QUERY

| Field | How the rows are sorted |
| --- | --- |
| `qty, count, amount, quantity` | By number. |
| `name, item` | By word, without regard to capitals. |
| `server` | By word. |
| Any other field | Not sorted: the order stays as it was. |

With ORDER BY and LIMIT together, the rows are sorted first, and LIMIT keeps the first ones of the sorted list. By default a QUERY reads up to 4,096 rows before it sorts them.

For SELECT and the other actions, WHERE, ORDER BY and LIMIT choose which item types the statement works on. The sort uses the item fields listed above, and LIMIT then takes the first item types.

## If something goes wrong

> [!PROBLEM] FROM is not valid on a read
> A QUERY has no FROM. Move the server into WHERE, as in QUERY items WHERE server = "Main Storage".

> [!PROBLEM] unknown item: <name>
> The WHERE kept no row for the item, so the reply says unknown item even though the item exists. Check the WHERE, the server after FROM and the stock with QUERY items.

> [!PROBLEM] expected a field name
> Put a field name before the operator, such as qty.

> [!PROBLEM] unknown operator: <text>
> Use one of the operators in the table above.

> [!PROBLEM] no server named '<server>'
> Check the server's name: QUERY servers lists them.

> [!SEE] jsc:iql_names_quotes_columns, jsc:iql_count, jsc:iql_limits
