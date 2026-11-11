---
section: iql_verbs
order: 365
---
# Names, quotes and columns

Every statement starts with a verb: the word for what the statement does, such as SELECT or QUERY. The verb is followed by the item or the object the statement works on. Clause words come after that. Each one adds a part: WHERE picks rows, ORDER BY sorts them and LIMIT cuts them short.

Verbs and clause words do not care about capitals, so query items and QUERY Items mean the same thing. A name with no colon is a vanilla item: cobblestone means minecraft:cobblestone. Read the three kinds of text below, because IQL reads each kind in its own way.

Table: The three kinds of text

| Kind | How you write it |
| --- | --- |
| Word | Any run of characters with no space, bracket, quote or comparison sign, such as iron_ingot or qty. |
| Number | A whole number, such as 64. A number with a percent sign is a percentage. |
| Quoted text | Text in single or double quotes, such as 'Old Faithful'. It may hold spaces. |

A sign such as = or < joins to a word that has no space around it. So qty<100 reads as three parts: qty, < and 100. Put a space on each side of a sign, and you avoid the problem.

## Where each kind is accepted

Table: What each place in a statement takes

| Where it stands in the statement | What IQL takes there |
| --- | --- |
| The item after the verb | A plain word. Quoted text is refused. |
| The name after FROM or TO | A plain word, with no space. |
| The name of a bus, interface or router after SET or RENAME | A word or quoted text, so 'Ore in' works. |
| The name of an anvil after SET NAME | Quoted text, at most 50 characters. |
| The value in WHERE or IF | A word, a number or quoted text. |
| The field name in WHERE or IF | A plain word. |
| The name of a saved statement, after CREATE or DROP | A plain word, at most 64 characters. |

Item names have no spaces, so an item is always a plain word. A bus name can have spaces, so you put it in quotes after SET: SET BUS 'Ore in' ON.

## Columns and fields

These are the tables and their columns:

Table: The tables and their columns

| Table | Columns |
| --- | --- |
| `items` | item, qty, server, tag, enchant |
| `servers` | name, used, free, era |
| `disks` | disk, server, tier, size, used |
| `operations` | id, verb, state, item, moved, requested, priority |
| `recipes` | recipe, where, machine, chance |
| `computers` | name, kind, era, state, os |
| `events` | event, name, state, starts, ends |
| `power` | server, row, watts, on |

An item has more fields than its table shows. A WHERE clause can compare any of these:

Table: Fields of an item

| Field | What it holds |
| --- | --- |
| `item` | The item's internal name, without its namespace, such as iron_ingot. |
| `name` | The display name the game shows. |
| `qty` | The quantity. count and amount mean the same. |
| `server` | The server the row belongs to. |
| `damaged` | true or false. |
| `durability` | The durability as a percent, from 0 to 100. An item that cannot wear out gives 100. |
| `tag` | The first tag of the item. |
| `enchant` | The enchantments, written as text. |

A comment starts with two dashes, --, and runs to the end of the line. A line that starts with the dashes does nothing when a file runs. The Management Studio can also ask you for a value when a script runs: its templates are in ([](jsc:studio_templates)).

> [!DEFINE] verb
> The word that starts a statement and says what it does, such as SELECT or QUERY.

> [!DEFINE] clause
> A word that adds a part to a statement, such as WHERE, ORDER BY or LIMIT. Clauses come after the item.

> [!DEFINE] column
> One field of a table that a QUERY shows. The same names work in WHERE and in ORDER BY.

## If something goes wrong

> [!PROBLEM] expected an item, got: 'Old Faithful'
> Item ids have no spaces. Type the item's id as one plain word, with no quotes.

> [!PROBLEM] expected a location after FROM, got: Ore in
> Type the name as one word, with no space, after FROM or TO.

> [!PROBLEM] A row you expect is missing from the answer.
> A field you typed does not exist in the table, so its comparison is false. Check the spelling against the tables above.

> [!SEE] jsc:query_and_select, jsc:clauses_priority_if_with
