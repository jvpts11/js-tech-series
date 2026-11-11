---
section: network_operation
order: 106
icon: jsc:server
---
# Services on a server bay

A server in a rack gets its role from the services installed on it. Its hardware decides how much a server can hold; the services decide what job it does. You choose the services each server runs.

A service answers only while its machine is up. Stop the server, and its services stop with it. Each server keeps its own list of services ([](jsc:server_racks)).

Table: The services of a server bay

| Service | What it does | Era |
| --- | --- | --- |
| Predictive Cache | Keeps the bay's most-wanted items staged in memory, cutting read latency by 15%. | Standard |
| Load Balancer | Spreads writes across the bay's drives instead of filling them one after another. | Legacy |
| Integrity Monitor | Re-reads the bay after a hot swap, so the network index does not doubt it. | Standard |
| Messenger Service | Keeps the network's conversations. | Standard |
| KnotHub | Keeps the source this network is still working on, revision by revision. | Standard |
| Soundfoundry Server | Streams the music catalogue to every Soundfoundry on the network. | Standard |
| Data Compressor | Packs the bay's items tighter: its drives hold a fifth more, and every read waits two ticks longer. | Standard |
| Backup Service | Backs the network up to a Tape Library or another server's drive, and puts it back. | Legacy |

Predictive Cache cuts the read latency of its bay by 15%, and a read never takes less than one tick. A read cache card in the same server adds its own cut on top of it.

> [!DEFINE] Predictive Cache
> A service of a server bay that keeps the bay's most-wanted items ready in memory, so you read them 15% sooner.

Integrity Monitor matters when you pull a drive out of a running bay. Without it, the index stops trusting that bay until somebody rebuilds the index with the reindex command. With it, the bay is re-read after the swap, and the index is not left in doubt.

> [!DEFINE] Integrity Monitor
> A service of a server bay that re-reads the bay after you swap a drive, so the network's index keeps trusting it.

Load Balancer changes where your new items land in its bay. Without it, the server fills its disks one after another. With it, each write goes to the emptiest disk first, so no single disk fills up while the others stand empty.

> [!DEFINE] Load Balancer
> A service of a server bay that sends each write to the bay's emptiest disk, instead of filling the disks in turn.

Data Compressor packs the items of its bay tighter, so its drives hold a fifth more. The price is time: every read from the bay waits two ticks longer. On a hard disk that is a small part of the wait and worth it; on NVMe, whose reads are quick, it triples the wait. The drives' labels do not change: the room comes from what each item takes on them, so a compressed drive taken out of its bay can show more than full.

> [!DEFINE] Data Compressor
> A service of a server bay that fits a fifth more items on its drives, at two ticks more on every read.

Backup Service backs the network up and puts it back. It is the subject of its own page ([](jsc:backups)).

## If something goes wrong

> [!PROBLEM] A service does not answer.
> Its server is off. Turn the server on.

## If something goes wrong

> [!PROBLEM] The index says a bay is unconfirmed after a hot swap.
> The bay has no Integrity Monitor. Install it on that bay, or rebuild the index by hand with the reindex command.
