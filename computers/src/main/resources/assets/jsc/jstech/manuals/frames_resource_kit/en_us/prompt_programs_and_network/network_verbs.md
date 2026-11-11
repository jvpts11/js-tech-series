---
section: prompt_programs_and_network
order: 245
---
# Network verbs: net, iql, interac and ssh

A network is the computers joined by data cables, with one Mainframe that coordinates them ([](jsc:what_a_network_is)). net describes it, iql asks it questions, interac works with its items, and ssh opens a shell on another computer of it.

> [!DEFINE] Mainframe
> The computer that coordinates a network. Each network has one.

Table: Verbs of the network

| Verb | Words after it, and what it does |
| --- | --- |
| `net (network)` | No words. Summarises the network: its id, whether a Mainframe is there, and how many servers, computers and subframes it has. |
| `iql (operation, op, sql)` | A statement. Runs one IQL statement on the network and prints the answer. |
| `interac` | Optional words. Works with the network's items: finds, fetches, puts back and crafts them. |
| `ssh` | A computer's name. Opens a shell on another computer of the network. |

## net

net prints the rows network, mainframe, servers, computers, subframes and indexed types. The mainframe row reads present, or MISSING when there is no Mainframe.

```command
id: jsc:command/mc_dos/net
```

## iql

iql runs one IQL statement on the network. Type iql, a space and the statement, such as iql SELECT 64 Cobblestone. A SELECT prints each item it finds as a row with its count, and it shows at most 64 rows. When nothing matches, it says no rows. A change, such as INSERT, prints the engine's words in green.

A statement that defines a view, a procedure or a job is answered as in the IQL Server Management Studio ([](jsc:studio_overview)). The grammar of IQL is taught in [](jsc:iql), so this page does not repeat it.

```command
id: jsc:command/mc_dos/iql
```

## interac

interac works the network's items. It finds what the network holds, fetches an item into your hands, puts items back and crafts them. Its words include get, put, craft, where, info, list and ops. Alone, interac takes the whole screen, and [[F10]] gives the terminal back.

Table: Options of interac

| Option | What it does |
| --- | --- |
| `/LOCAL` | Leaves what interac takes in this computer, not in your hands. |
| `/S:COUNT` | Orders a listing by how much there is. --sort name orders it by name. |

For example, interac get 42 cobblestone takes 42 cobblestone into your hands, through this computer. An item name can be short, such as cobblestone, or full, such as minecraft:cobblestone. When a name fits more than one thing, interac lists them and asks you to say which one.

This page covers the commonest verbs. The Operations behind interac's work are described in [](jsc:operations_catalogue).

```command
id: jsc:command/mc_dos/interac
```

## ssh

ssh, the secure shell, opens a shell on another computer of this network, so that you can type commands there. Type ssh and the name of the computer, as the list shows it. The prompt lists the reachable computers, each with its node number, or (offline) when it is down. Type exit to come back ([](jsc:shell_verbs)).

```command
id: jsc:command/mc_dos/ssh
```

## If something goes wrong

> [!PROBLEM] not on a network
> The computer must be on a data network: a data cable to a router, or to the Mainframe ([](jsc:data_cables)).

> [!PROBLEM] iql says syntax: and then the parser's words.
> The message names the word where the parser stopped. Fix that word. The grammar is in [](jsc:iql).

> [!PROBLEM] iql SELECT says no rows.
> Nothing matches the name. Use interac list with part of the name to check its spelling.

> [!PROBLEM] no other computers reachable on this network
> No other computer of this network is up and reachable. Try again when one is.

> [!SEE] jsc:what_a_network_is, jsc:first_network, jsc:iql
