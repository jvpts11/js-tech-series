---
section: iql_start
order: 364
---
# Your first queries, step by step

This entry takes you from a network with nothing typed yet to your first answers and your first work. Each step shows what you type and what the network says back. If your reply differs, read the problems at the end.

## Before you start

1. Join the computer to the network with a data cable ([](jsc:first_network)).
2. Turn on the Mainframe ([](jsc:mainframes)).
3. Type iqlengine status and press [[Enter]].

The reply says "Midsoft IQL Server: running" when the server is ready to answer. If it says stopped, start it with iqlengine start ([](jsc:engine_services)).

> [!DEFINE] server
> A computer on the network that holds items on its disks.

Type net to see a summary of the whole network. It has headings for the network, the Mainframe, the servers, the computers, the subframes and the types it indexes.

## Asking questions

Table: Your first reads

| You type | The reply |
| --- | --- |
| `iql QUERY disks` | A row for each disk, or "no rows" |
| `iql QUERY servers` | A row for each server |
| `iql QUERY items WHERE qty > 100` | The items held in amounts over 100 |
| `iql QUERY items ORDER BY qty DESC LIMIT 2` | The 2 rows with the largest quantity |

Each row is one line. Its name and details are on the left, and its quantity is on the right. The command line shows at most 64 rows, so a longer answer is cut short.

> [!DEFINE] table
> A list that a QUERY reads, such as disks, servers or items.

The three tables of these first reads have these fields. Each field is one piece of information about a row.

Table: The fields of three tables

| Table | Fields |
| --- | --- |
| disks | `disk, server, tier, size, used` |
| servers | `name, used, free, era` |
| items | `item, qty, server, tag, enchant` |

ORDER BY sorts the rows by one field before they are shown. DESC reverses the order. LIMIT 2 stops the answer after 2 rows. The other tables are listed in [QUERY and SELECT](jsc:query_and_select).

## Taking items out

1. Type iql SELECT 64 iron_ingot and press [[Enter]].
2. Read the reply: it says the SELECT is queued, and where the items go.
3. Type iql CRAFT 64 torch and press [[Enter]].
4. Read the reply: it says the CRAFT is queued.

Table: Your first work

| You type | The reply |
| --- | --- |
| `iql SELECT 64 iron_ingot` | `SELECT queued: 64 Iron Ingot -> local storage` |
| `iql CRAFT 64 torch` | `CRAFT queued: 64 Torch` |

SELECT with no FROM takes the items from anywhere on the network. To take them from one server, write FROM and the server's name, as QUERY servers lists it. The reply names the item by its display name, and the items reach local storage: this computer's own storage.

CRAFT makes items, and it works only for items the network has a pattern for ([](jsc:how_autocrafting_works)).

## Watching the work

Type interac ops at the prompt to list the work that is still running. The Network Manager program also lists the Operations in flight ([](jsc:network_manager)).

## If something goes wrong

> [!PROBLEM] The reply says "syntax: expected an item".
> Write the item's name after the action, such as iron_ingot.

> [!PROBLEM] The reply says "syntax: percentage quantities are not supported yet".
> Write a number of items instead, such as 50 items.

> [!PROBLEM] The reply says "syntax: SELECT has no TO destination".
> SELECT brings items to you. Leave out TO. To send items between two places, use MOVE.

> [!PROBLEM] The reply says "syntax: DELETE exports out of the network and needs a TO destination".
> Add TO, followed by the place where the items go.

> [!PROBLEM] The reply says "syntax: MOVE is internal and needs both FROM and TO".
> Give both places, FROM where the items come from and TO where they go.

> [!PROBLEM] The reply says "syntax: FROM is not valid on a read".
> A query only reads. Leave out FROM, or use an action that moves items.

> [!PROBLEM] The reply says "syntax: unknown priority level".
> Use one of the levels the reply lists: LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH or HIGH.

> [!PROBLEM] The reply says "syntax: unknown verb", followed by a word.
> Check the spelling of the action word. The words that exist are in [the catalogue](jsc:operations_catalogue).

> [!PROBLEM] The reply says "unknown item", followed by the name.
> The network does not know that name. Check the spelling, or write the full name, such as minecraft:iron_ingot.

> [!PROBLEM] The reply says "no server named", followed by the name.
> No server has that name. Use QUERY servers to see the names.

> [!PROBLEM] The reply says "syntax: expected an object to query".
> Name what to query after QUERY, such as disks or items.

> [!PROBLEM] The reply says "no pattern crafts", followed by the item's name.
> The network has no pattern for that item ([](jsc:how_autocrafting_works)).

> [!PROBLEM] The reply says "Network Operations Service unavailable".
> The Midsoft IQL Server is stopped. Start it with iqlengine start.

> [!PROBLEM] The reply says "the network has no running Mainframe".
> Turn the Mainframe on.

> [!SEE] jsc:query_and_select, jsc:operations_catalogue, jsc:iql_errors
