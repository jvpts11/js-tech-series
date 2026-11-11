---
section: clusters
order: 60
---
# Subframes

A Subframe is a computer that helps the Mainframe of its network carry the work. When your Mainframe is alone, it does all the work itself, with its own capacity and queues.

A Subframe lends a share of its capacity. By default that share is 60%, set by the Subframe share in the mod's config. It lends its queues too, which are its graphics cards, so a Subframe without graphics cards lends capacity only.

A Subframe works only for a Mainframe that runs the same engine as it does ([](jsc:engines)). A Subframe without a Mainframe lends nothing.

You make a Subframe out of a Mainframe by giving its engine the Subframe's role: tick Subframe on the Services tab of its Network Manager, or type iqlengine role subframe at its prompt. It then counts for no network conflict, keeps no index of its own and lends to the network's orchestrator. Tick Orchestrator, or type iqlengine role orchestrator, to make it run the network again; if the network already has an orchestrator, that is a network conflict.

> [!DEFINE] Orchestrator
> The Mainframe that runs its network: its index, its Operations and its engine.

When the orchestrator falls and nobody runs the network for 60 ticks, the network is taken over. A Mainframe in failover goes first if there is one; otherwise a Subframe that runs the same engine, then one that runs another, the lowest-placed first. The network's engine becomes the engine of the one that took over, and the Operations the fallen orchestrator was carrying are lost. The network's event log says who stopped, who took over, which engine runs now and how many Operations were lost.

The Services tab lists the Subframes with what each lends, marks one that runs another engine as a reserve, and says who takes the network over if the orchestrator falls.

## If something goes wrong

> [!PROBLEM] The screen says "No Subframes on this network."
> The network has no Subframe, so the Mainframe does all the work by itself.

> [!PROBLEM] The screen says a Subframe will take no work until it runs the same engine too.
> Make it run the same engine as its Mainframe ([](jsc:engines)).

> [!SEE] jsc:states_and_queues, jsc:engines
