---
section: storage
order: 121
icon: jsc:external_storage_bus
---
# The External Storage Bus

An External Storage Bus lets the network use a chest, a tank or a machine as storage, with no disk in it. You put the bus on a data cable beside the inventory, and the network counts what the inventory holds as its own storage.

> [!DEFINE] Inventory
> Any chest, tank or machine that holds items or fluid, and that a bus can face.

Its tooltip reads "Right-click a data cable beside an inventory; the network uses the inventory as its storage". A bus mounts on cables of its own era or an earlier one.

Its storage is much slower than the network's own disks. The speed below shows by how much.

The Vintage, Legacy, Transition and Advanced versions carry their era in their names, such as "Vintage External Storage Bus". The Standard version has the plain name.

## What each era can do

Table: The External Storage Bus by era

| Era | Filter and access | Priority | Tags and looser matching |
| --- | --- | --- | --- |
| Vintage | none | no | no |
| Legacy and Transition | yes | no | no |
| Standard | yes | yes | no |
| Advanced | yes | yes | yes |

The External Storage Bus has no conditions in any era. Conditions belong to the Import Bus and the Export Bus ([](jsc:bus_conditions)).

## Access and filter

Access sets what the network may do with the inventory. It starts at the Legacy era, and a Vintage bus always reads and writes.

Table: Access of the bus

| Setting | What the network does |
| --- | --- |
| READ AND WRITE | Takes items out of the inventory and puts items into it (the default). |
| READ ONLY | Only takes items out, and never fills the inventory. |
| WRITE ONLY | Only puts items in; what is in the inventory is never taken or seen. |

The filter decides which items the bus lets through. With no item listed, the bus lets everything through. With items listed, only those pass, or with "ALL BUT THESE" everything except them.

The bus window shows what the inventory holds under "HOLDS", as slots and how many are used, with the note "shown to the network as its storage".

## Speed

The bus moves a tenth of what its cable carries each tick, and at least one item. Its read latency is 100 ticks, which is 5 seconds: ten times the 10 ticks of a hard disk (HDD). A solid-state drive (SSD) answers about 33 times faster than the bus, and an NVMe disk 100 times faster.

The bus window says "10x slower than the network's own storage." That holds against a hard disk only. A server of solid-state or NVMe disks is faster still.

The network looks at the inventory again every 10 ticks. So a change in the inventory can reach the network's list of items up to 10 ticks late.

## Room and refusals

The room the bus offers is the inventory's free room. A READ ONLY bus offers none. An INSERT skips the bus when it is READ ONLY, or when its filter does not let the item through.

## If something goes wrong

> [!PROBLEM] The window says "no inventory before it".
> The bus faces nothing. Put it on a data cable beside an inventory.

> [!PROBLEM] The window says "on no network".
> Join the bus's cable to a network ([](jsc:data_cables)).

> [!PROBLEM] The window says "the chest is full".
> The inventory has no free room. Take items out of it.

> [!PROBLEM] The network never puts items into the bus.
> The bus is READ ONLY. Set it to READ AND WRITE.

> [!PROBLEM] The network never takes items from the bus, and never sees them.
> The bus is WRITE ONLY. Set it to READ AND WRITE or READ ONLY.

> [!PROBLEM] The bus has no READ ONLY setting.
> Access starts at the Legacy era. Use a Legacy or newer External Storage Bus.

> [!SEE] jsc:buses, jsc:storage_priority, jsc:storage_trouble
