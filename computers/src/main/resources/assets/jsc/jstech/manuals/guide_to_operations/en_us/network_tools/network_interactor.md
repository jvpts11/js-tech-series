---
section: network_tools
order: 86
icon: jsc:personal_computer
---
# The Network window

The Network window reaches the network's storage and its work from this computer. Its desktop icon is called Network, and its window is titled Network Interactor. It comes with every desktop, and it does what the MC-NET terminal does ([](jsc:mc_net)).

From it you ask the network for items, take items from local storage into your inventory, send items from local storage back to the network, and ask the network to craft items. The items you ask for come into the local storage of this computer first.

> [!DEFINE] Local storage
> The disk storage of this computer, as opposed to the storage the network holds.

> [!DEFINE] Request
> To ask the network to put items into the local storage of this computer.

> [!DEFINE] Favourite
> An item you star, so that it stays in its own tab.

## The window

Table: Parts of the Network window

| Part | What it does |
| --- | --- |
| Toolbar | The search field, and the buttons Sort, Mod and Category. |
| Tabs | Six tabs, and the Favourites tab at the end. |
| Grid | The items of the tab, in a well of cells with a caption over it. |
| Details panel | The item you chose, its actions and its details. |
| Inventory band | The slots of your inventory, at the foot of the window. |
| Grips | Two handles that reshape the window: one adds columns to the grid, and the other shows from 1 to 4 rows of your inventory. |
| Status bar | The Mainframe's state, the counts and a storage gauge. |

Drag a grip to change the size. The window remembers its size.

## The tabs

Table: Tabs of the Network window

| Tab | What it lists | When it is empty |
| --- | --- | --- |
| Status | The network card. | Never empty. |
| Local | Items on this computer's own disks. | Nothing on this computer's disks. |
| Network | Every item the network holds. | Nothing on the network. |
| Crafting | Items the network can craft from its patterns. | No patterns on the network. |
| Operations | The network's Operations, running ones first. | No operations on the network. |
| Favourites | The items you have starred. | Nothing starred yet. |
| Federated | The items of a network federated with this one, after the Network tab. | No network is federated with this one. |

Press [[F]] on an item, or its star in the details, to star it. The status card on the Status tab shows the network's state. Its header says This network, and then Mainframe online or Mainframe offline. Below it, a STORAGE gauge shows the disk space in use and the items held. Then come the counts of item types, servers, craftable items and favourites, and the count of Operations running.

## Requests and the inventory

Press [[Enter]] on an item, or click it, to open its dialog. On the Network tab, Request asks the network to put the items into the local storage of this computer. On the Local tab, To Inventory takes items out of local storage into your inventory, as far as your inventory has room. To Network sends items from local storage to the network, and anything the network does not take comes back to local storage.

Adv opens an advanced panel, where you choose the servers to pull from and where the items go. Fluids and chemicals cannot go into your inventory.

The quantity buttons move the amount by 1, 10, 100 or 1,000, up or down, and Max takes the most allowed. You can also type digits, and [[Backspace]] deletes the last one. With several items chosen, each item is asked for the quantity, or what the network holds of it if that is less.

The Prio button sets the [priority](jsc:priorities) of the request. It cycles through LOW, MED-, MED, MED+ and HIGH, and it starts at the default, MEDIUM.

## Searching, filtering and sorting

Press [[/]] on the grid to move to the search field. Type part of a name, and the list shows the items whose names match. [[Esc]] leaves the field. The Sort button cycles A-Z, MOST and LEAST: alphabetical, largest total first, or smallest total first. The Mod and Category buttons open menus that narrow the list to one mod, or one category of items.

The search and the filters apply to the tab you are on, not to the Status or the Operations tab.

Table: Keys on the grid

| Key | What it does |
| --- | --- |
| `Arrow keys` | Move the dotted cell. |
| `Shift` | With an arrow, extends the selection. |
| `Enter` | Opens the dialog for the selected items. |
| `C` | Opens the Craft dialog for the cell's item, if it can be crafted. |
| `F` | Stars or unstars the selection. If all are starred, all are unstarred. |
| `Ctrl+A` | Selects every item of the tab. |
| `/` | Moves to the search field. |

## Crafting

The Crafting tab lists the items the network can craft from its [patterns](jsc:pattern_encoders). A pattern is the recipe the network knows, and a [Crafting Computer](jsc:crafting_computers) loads them.

Press [[C]], or [[Enter]] on a craftable item, to open the Craft dialog. It shows a plan: the recipe it uses, the machines and stages, and what each input costs in stock. Craft makes the whole amount, Partial makes what can be made now, and [[Enter]] makes the whole amount too.

When an item can be made in more than one way, up to three recipe cards sit side by side. [[Left]] and [[Right]] change the card, and the computer remembers the choice for each item. The estimate, marked EST, shows a time, or EST -- while the plan is still being worked out.

## Updating an item

The dialog of a single item also has an Update... button. It opens a window with three tabs: Enchant, Smelt and Repair. A tab is greyed out when this computer has no card for it, or when the item does not take it. The cards are the personal-use cards of the [Workshop](jsc:workshop). The window closes once the network has taken the update.

## Operations

The Operations tab lists the network's [Operations](jsc:what_operations_are). The running ones come first, then the recent log, which keeps the last 32 Operations. A row shows its state in short form:

Table: States in the Operations tab

| Short | Full name |
| --- | --- |
| `done` | Completed |
| `part` | Completed (partial) |
| `fail` | Failed |
| `run` | Processing |
| `wait` | Waiting |
| `lock` | Resource locked |
| `pend` | Pending |
| `drop` | Discarded |

Open a row to see how much of the Operation has moved, and its parts: one sub-operation for each machine that works on it, or its sources. With a Supercomputer, the caption also shows how many parallel crafts run, out of the number the Supercomputer has.

## Federated networks

When this network is federated with another through [WAN Gateway Computers](jsc:wan_gateways), the Federated tab shows that network's items in a view of their own, never mixed with this network's. Its card says over which port and line it is reached, what this network may do there, and what is on it. With several, Next network steps through them.

Request takes items from that network into the local storage of this computer, and Craft there has it craft something, which stays on that network. Drop a stack on its grid to send it there. The far network decides which of these it allows, in its own gateway's BorderLink, and each crosses the line as an Operation. On the Operations tab its row says which network it went to, and its details say where it has got to.

## If something goes wrong

> [!PROBLEM] The status card says Mainframe offline.
> This window cannot reach the network. Place the computer on a network whose Mainframe is running.

> [!PROBLEM] The Network tab says Nothing on the network.
> The network's disks hold nothing yet. Put items on the network first.

> [!PROBLEM] The Crafting tab says No patterns on the network.
> No Crafting Computer holds a pattern yet. Load .craft files on a Crafting Computer.

> [!PROBLEM] The Crafting tab lists fewer items than you expect.
> The tab shows only what the patterns allow. Load more patterns on a Crafting Computer.

> [!PROBLEM] The Craft dialog shows EST --.
> The plan is still being prepared, or no route exists yet. Wait a moment, then look again.

> [!PROBLEM] The Federated tab says the far network does not let this one see its items.
> Its own gateway decides that. Turn on See our items for this network's port, in that gateway's BorderLink.

> [!SEE] jsc:mc_net, jsc:what_a_network_is, jsc:states_and_queues, jsc:crafting_computers
