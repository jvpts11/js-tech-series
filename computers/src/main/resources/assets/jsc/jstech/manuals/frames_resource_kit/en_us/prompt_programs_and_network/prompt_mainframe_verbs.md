---
section: prompt_programs_and_network
order: 246
---
# Looking after the network from the prompt

A few verbs work on the network itself, so the prompt offers them only on the computer that keeps that part of the network. Type one anywhere else, and the prompt says it does not know the command.

Table: Where each verb is offered

| Verb | Offered on |
| --- | --- |
| `iqlengine` | The network's Mainframe |
| `services` | The network's Mainframe |
| `analyze` | The network's Mainframe |
| `reindex` | The network's Mainframe |
| `vacuum` | The network's Mainframe |
| `mirror` | The network's Mainframe |
| `cluster` | A Cluster Management Computer |
| `gateway` | A computer with ports, for its Network Gateways |
| `borderlink` | A WAN Gateway Computer with Nouvell BorderLink |
| `msgsvc` | A server running the Messenger Service |

The Mainframe's verbs are there only while it is on a network. A Mainframe running MC-DOS or Frames offers them at its Command Prompt.

## The engine and the services

iqlengine starts and stops the Midsoft IQL Server, the engine that plans the network's work ([](jsc:midsoft_iql_server)). Type iqlengine start, iqlengine stop or iqlengine status. At a Mainframe's own prompt, iqlengine role subframe or iqlengine role orchestrator gives its engine that role ([](jsc:subframes)). services lists the network's services, each with its state. The prompt offers both only while the Midsoft IQL Server is installed on the Mainframe.

```command
id: jsc:command/mc_dos/iqlengine
```

```command
id: jsc:command/mc_dos/services
```

## The storage index

The Mainframe keeps the storage index: its list of which disks hold which items, so the network finds an item without reading every disk. Three verbs look after it.

> [!DEFINE] Storage index
> The Mainframe's list of which disks of the network hold which items.

> [!DEFINE] Ghost entry
> An item the storage index still lists, though no disk holds it any more.

Table: Looking after the index

| Verb | What it does | What it says |
| --- | --- | --- |
| `analyze` | Checks the index against the disks. | `ANALYZE complete - 12 types reconciled` |
| `reindex` | Reads every disk again and builds the index anew. The new index takes over a tick or two later. | `REINDEX started - rebuilding the catalog from disks` |
| `vacuum` | Clears the ghost entries. | `VACUUM freed 3 ghost entries` |

When you pull a drive out of a running bay that has no Integrity Monitor, the index stops trusting that bay until you type reindex ([](jsc:network_services)). Learning IQL explains the same three jobs as statements ([](jsc:maintenance_analyze_vacuum_reindex)).

```command
id: jsc:command/mc_dos/analyze
```

```command
id: jsc:command/mc_dos/reindex
```

```command
id: jsc:command/mc_dos/vacuum
```

## The Mirror

mirror install puts the Mirror on the Mainframe: the service that pckmgr fetches packages from ([](jsc:mirror)). mirror status says whether it serves. It answers serving, installed (Mainframe off) or not installed.

```command
id: jsc:command/mc_dos/mirror
```

## Clusters and gateways

cluster does at the prompt what the Cluster Manager does in its window ([](jsc:cluster_management_computers)). It lists the network's supercomputers and datacenters, shows the nodes of one, turns them on or off, and installs a system or a program on them.

gateway does what the Gateway Manager does for the Network Gateways on this computer's ports, the bridge to ComputerCraft ([](jsc:network_gateway)). It lists them, shows the status, permissions, computers and log of one, and changes its settings.

borderlink does on a WAN Gateway Computer what Nouvell BorderLink does in its window ([](jsc:wan_gateways)). It lists the four ports, shows one, sets what the far side may do here, and shows the gateway's log: borderlink port 2 relay on.

```command
id: jsc:command/mc_dos/cluster
```

```command
id: jsc:command/mc_dos/gateway
```

```command
id: jsc:command/mc_dos/borderlink
```

## The Messenger Service's conversations

msgsvc, at the prompt of the server that runs the Messenger Service, looks after the conversations it keeps ([](jsc:messenger)). msgsvc list numbers them, msgsvc delete 2 deletes the second, msgsvc delete --all deletes every one, and msgsvc status says what the service keeps and weighs. A deleted conversation leaves every Messenger at once.

```command
id: jsc:command/mc_dos/msgsvc
```

## If something goes wrong

> [!PROBLEM] The prompt says it does not know the command.
> Type it at the prompt of the computer the table names for it.

> [!PROBLEM] iqlengine says the Midsoft IQL Server is already running.
> It is serving already. Type iqlengine status to check it.

> [!PROBLEM] iqlengine says the Midsoft IQL Server is already stopped.
> Type iqlengine start to start it.

> [!PROBLEM] mirror says the Mirror is already installed.
> Type mirror status to see whether it serves.

> [!SEE] jsc:mainframes, jsc:engine_services, jsc:mirror
