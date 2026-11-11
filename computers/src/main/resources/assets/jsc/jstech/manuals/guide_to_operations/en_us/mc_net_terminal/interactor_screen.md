---
section: mc_net_terminal
order: 112
icon: jsc:monitor
---
# The Interactor

The Interactor is the network's full-screen window on MC-NET. On its own it takes the whole terminal. Its headings are the network, the servers, what is held back, the work in flight and what you have starred, and a list sits under them.

Arrows move through the list. Tab goes to the next heading, and typing looks for something. Along the foot of the screen are ten keys, labelled 1 to 10. Each key is the function key of the same number: press [[F1]] for key 1, up to [[F10]] for key 10.

Table: The keys of the Interactor

| Key | What it does |
| --- | --- |
| `F1` | Help. |
| `F2` | Get: takes items from the network into your hands. It asks for the quantity first. |
| `F3` | Put: puts what is in your hands into the network. |
| `F4` | Craft: asks for a quantity, then starts a craft of the item. |
| `F5` | Lock: holds the selected item back. It is then listed under held back. |
| `F6` | Free: unlocks the selected item. |
| `F7` | Fav: stars the selected item, so that it is listed under starred. |
| `F8` | Stop: stops the selected operation on the Ops heading. On any other heading it says that only operations can be stopped. |
| `F9` | Find: looks for an item as you type its name. |
| `F10` | Quit: gives the terminal back. |

The same words work at the prompt, and they do the same things. Each one is shown here with what it does.

Table: Words at the prompt

| Words | What they do |
| --- | --- |
| `interac` | Opens the whole network on the whole screen. |
| `interac list stone --sort count` | Lists stone, with the largest amount first. |
| `interac get 42 cobblestone` | Takes 42 cobblestone into your hands. |
| `interac put hand` | Puts what is in your hand into the network. |
| `interac where diamond` | Says where diamond is held. |

The option --to local leaves what is taken in this computer, and not in your hands. The option --sort count orders a listing by how much there is, and --sort name orders it by name.

You can name an item the way you would say it: cobblestone, minecraft:cobblestone, or "oak log". A name that fits more than one thing is answered with the things it fits, so that your next line can name the one you meant.

With words, the Interactor does the one thing you asked and gives the prompt back. An operation that takes time prints its number, and its result appears before the next prompt.

## If something goes wrong

> [!PROBLEM] The name matches nothing on the network.
> Check the spelling, or name the item another way, such as minecraft:cobblestone.

> [!PROBLEM] The network holds nothing.
> Put items in first, with put.

> [!PROBLEM] interac is not offered at the prompt.
> The machine has no network. interac, ssh and mirror are not offered on a machine with no network ([](jsc:network_members)).

> [!PROBLEM] The Stop key says that only operations can be stopped.
> Stop works on the Ops heading only. Select an operation there first.
