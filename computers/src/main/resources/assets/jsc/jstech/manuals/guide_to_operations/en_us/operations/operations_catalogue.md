---
section: operations
order: 133
icon: jsc:mainframe
---
# The catalogue of Operations

Here is every Operation the network knows, and what it does. Most of their names come from the commands of databases, so if you know a little SQL, you already know many of them.

> [!DEFINE] IQL
> Item Query Language: the network's own language, in which you type the work that you want done, such as SELECT 64 iron_ingot.

> [!DEFINE] statement
> One IQL instruction: an action, a question or a maintenance command.

> [!DEFINE] verb
> The word that starts an IQL instruction, such as SELECT or QUERY.

> [!DEFINE] personal-use card
> A workshop card in the computer that asks for an UPDATE. The card does the work for that computer.

Table: Every Operation

| Operation | What it does | An example |
| --- | --- | --- |
| `SELECT` | Takes items out of storage and brings them to the computer that asked. | `SELECT 64 iron_ingot` |
| `INSERT` | Puts items that the computer holds into the network's storage. | `INSERT 64 cobblestone` |
| `MOVE` | Moves items between two servers, or between a server and a named bus. | `MOVE 64 cobblestone FROM <server> TO <server>` |
| `DELETE` | Sends items out of the network, through the bus named after TO. Destroys nothing. | `DELETE 64 cobblestone TO '<bus name>'` |
| `DROP` | Destroys items for good. | `DROP 64 cobblestone` |
| `CRAFT` | Makes items the network crafts, in machines or in steps. | `CRAFT 64 torch` |
| `UPDATE` | Changes an item the network holds, with a personal-use card. | `UPDATE 1 iron_pickaxe SET REPAIR` |
| `ANALYZE` | Keeps the index up to date. It looks only at the servers that changed, and drops the entries of servers that left. | `ANALYZE` |
| `REINDEX` | Rebuilds the whole index from every disk. | `REINDEX` |
| `VACUUM` | Removes the index entries that hold nothing, or point at a server that is no longer on the network. | `VACUUM` |
| `LOCK` | Reserves items, so that other Operations wait for them. | `LOCK 64 iron_ingot` |
| `UNLOCK` | Releases a reservation. | `UNLOCK iron_ingot` |

Each name in the first column is an IQL verb ([](jsc:query_and_select)). QUERY and COUNT only read the network, so they are not Operations. The Learning IQL book explains each verb in its own entry: UPDATE in [](jsc:update_cards), and LOCK in [](jsc:lock_unlock).

The Network program sends most of them for you when you click, so you do not have to type them. In IQL you can say exactly what you want ([](jsc:iql)).

UPDATE works in three stages. The item is taken out of the network with a SELECT, the card works on it, and the result is put back with an INSERT. The card is a workshop card in the computer that asked ([](jsc:workshop)).

Table: The cards of UPDATE

| Card | Its UPDATE actions |
| --- | --- |
| Furnace Card | `SMELT` |
| Enchanting Card | `ENCHANT` |
| Anvil Card | `REPAIR, COMBINE, NAME` |

Whoever asked pays the card's experience price, except for smelting. A name with no prefix, such as iron_ingot, means the Minecraft item of that name.

> [!WARNING] DROP destroys items for good, and nothing brings them back.

## If something goes wrong

> [!PROBLEM] The result says "the network was not holding the", followed by an item.
> The network does not hold that item. Store it first ([](jsc:storage_servers)), or check its name.

> [!PROBLEM] The result says the card could not do it: nothing to change, too expensive or too few levels.
> Check the item. A card needs something to change, and the player who asked needs enough levels for the work.

> [!PROBLEM] The result says whoever asked for it was not here to pay.
> The player who asked has to be there to pay the experience. Ask again with them present.

> [!SEE] jsc:operation_states, jsc:iql
