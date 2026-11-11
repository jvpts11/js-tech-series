---
section: storage
order: 120
icon: jsc:disk_vaultis_trench_20m
---
# Storage priority

When the network stores items, it must choose a storage for them, and the storage priority decides the order. The storage with the highest priority fills first. This is not the priority of an Operation ([](jsc:states_and_queues)), which orders the jobs themselves.

> [!DEFINE] Storage priority
> The number that orders the storages when items go in. The highest fills first, and the network's own storage counts as priority 0.

## When items go in

1. Look at the free room of every storage, and skip any that refuse the item.
2. Give the items to the storage with the highest priority, up to its free room.
3. Between storages with the same priority, prefer the faster one, then the one with more room.
4. Give the rest to the next storage in that order, and write them.

Some items go back to the server they came from first, as far as that server has room. This is how the results of a craft return ([](jsc:asking_for_a_craft)).

## When items come out

Taking items out uses another order: the fastest storage first, then the fuller one.

Table: Read latency of the network's disks

| Disk | Read latency |
| --- | --- |
| Hard disk (HDD) | 10 ticks |
| Solid-state drive (SSD) | 3 ticks |
| NVMe (non-volatile memory express) | 1 tick |

> [!DEFINE] Read latency
> The ticks a storage waits before it gives items out. Fewer ticks is faster.

These are the default values, and the mod's configuration can change them.

## Priority on buses

Only an External Storage Bus has a storage priority, and only from the Standard era on. Servers, racks and computers always count as 0. You set a bus's priority from -99 to 99. The same number also decides which bus claims an item first ([](jsc:bus_keep_max_and_priority)).

A bus with a negative priority fills after the network's own storage, and a bus with a positive one fills before it.

## Faster reads and writes

A read cache card, the Vaultis Cache of the era, in a server's expansion slot cuts the read latency of the server's disks, by more in each newer era ([](jsc:raid_arrays)). The Predictive Cache service cuts it by 15%. Their cuts add together.

Inside one server, the Load Balancer service changes the write order. New items go to the emptiest disk first, instead of filling the disks in turn ([](jsc:network_services)).

## If something goes wrong

> [!PROBLEM] The bus has no priority setting.
> Priority starts at the Standard era. Use a Standard or newer External Storage Bus.

> [!SEE] jsc:external_storage_bus, jsc:states_and_queues, jsc:network_services
