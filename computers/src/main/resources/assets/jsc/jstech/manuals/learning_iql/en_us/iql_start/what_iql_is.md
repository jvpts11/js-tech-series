---
section: iql_start
order: 362
---
# What IQL and Operations are

Your items are not kept in one chest. They sit on the disks of the network: a set of computers and devices joined by data cable to one Mainframe. To get an item out, you do not open a box. You ask the network for it, and the network fetches it for you.

> [!DEFINE] network
> Every computer and device joined by data cable to one Mainframe.

> [!DEFINE] Mainframe
> The one computer of a network that runs its work and keeps track of it.

You ask in IQL, the Item Query Language. IQL is the network's own language, and it looks like the languages of databases: a word for the action, then the item and how many. Its words come from SQL (Structured Query Language), the language that databases speak.

> [!DEFINE] IQL
> The network's own query language: the words you type to ask it for items and to tell it what work to do.

Each line you type is a statement. A statement either asks a question, or it asks for work. QUERY asks a question, and SHOW means the same. A question gets its answer as rows, and it changes nothing.

> [!DEFINE] statement
> One complete IQL instruction, typed on its own line.

> [!DEFINE] query
> A question put to the network, which it answers with rows and without changing anything.

> [!DEFINE] row
> One line of an answer, such as one disk or one item, with its details.

When a statement asks for work, the network turns it into an Operation. An Operation is one action the network carries out. It waits its turn, and the network works on it over game ticks.

> [!DEFINE] Operation
> One action the network carries out, from the moment it is asked until it ends.

> [!DEFINE] tick
> One beat of the game. 20 ticks make a second.

Take SELECT 64 iron_ingot. It asks the network to bring 64 iron ingots to you. That is one Operation. The reply only says that the Operation is queued. The items reach you as the network works it.

Table: The parts of a statement

| Part | What it is | Example |
| --- | --- | --- |
| Action | The word that says what to do, such as SELECT or QUERY | `SELECT` |
| Quantity | How many. You may leave it out. | `64` |
| Item | The item's name, as one word. | `iron_ingot` |
| Extra words | Words such as WHERE, which narrow the answer. | `WHERE qty > 100` |

The quantity is optional. Leave it out with SELECT, and the network brings all it holds of that item. The word ALL means the same. A quantity is a whole number: 50% is refused.

Write an item's name as one word, with an underscore for each space. iron_ingot means minecraft:iron_ingot, the iron ingot of Minecraft. A name with a colon, such as mod:item, is taken as the full name.

Each Operation also has a priority. It is MEDIUM unless you say otherwise. Write PRIORITY and a level such as HIGH at the end of a statement to set it ([](jsc:priorities)).

An Operation ends in one of eight states, and the network tells you which one ([](jsc:operation_states)).

## If something goes wrong

> [!PROBLEM] The reply says "the network has no running Mainframe".
> The Mainframe is off, or it is not on this network. Turn it on, or join the computer to its network ([](jsc:first_network)).

> [!PROBLEM] The Operation stays PENDING.
> The Mainframe is busy, off, or has no processor time left for it. Wait a little, or give the Mainframe more processors or graphics cards ([](jsc:mainframes)).

> [!PROBLEM] The Operation ends RESOURCE_LOCKED.
> Another Operation, or a LOCK, holds an item it needs. Wait for the other Operation to finish, or read about locks ([](jsc:lock_unlock)).

> [!PROBLEM] The Operation ends COMPLETED_PARTIAL.
> The network had less than you asked for. Ask for a smaller quantity. QUERY items shows how much of each item the network holds.

> [!SEE] jsc:operations_catalogue, jsc:where_to_type_iql, jsc:query_and_select
