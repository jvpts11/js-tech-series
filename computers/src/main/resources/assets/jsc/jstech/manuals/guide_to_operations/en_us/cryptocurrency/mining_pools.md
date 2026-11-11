---
section: cryptocurrency
order: 116
icon: jsc:server
---
# Mining Pools and the Mining Panel

Mining alone, you find a block now and then. A Mining Pool gathers the miners of a network that choose it and mines with all of them together; when it finds a block, it pays each miner its share. That is a steady income instead of a block now and then.

> [!DEFINE] Mining Pool
> A service on a rack server that mines with its miners together and shares each block's reward by the work each gave.

> [!DEFINE] Mining Panel
> A desktop program that shows the pools of a network: their hashrate, their miners and the blocks they found.

Install the Mining Pool on a server in a rack of the network, from the Transition on ([](jsc:network_services)). A miner chooses it by its name, with miner --pool and the name, or with the pool button of an ASIC's window.

When the pool finds a block, it shares the reward by the work each miner gave since its last block, the miner's hashrate times the time it mined, less the pool's fee. The fee goes to the owner's address; the owner sets it from 0 to 5%, and it starts at 1%.

Table: The Mining Panel's tabs

| Tab | What it shows |
| --- | --- |
| Pool | The pool's hashrate, what it pays an hour, a day and a week, the blocks it found and the difficulty. |
| Miners | Each miner, its kind, the address it pays, its hashrate and its share. |
| Blocks | The blocks the pool found, and who found each. |
| Settings | Only at the pool's own server: its name, its fee, its owner's address, who may join, and the miners asking to. |

## If something goes wrong

> [!PROBLEM] A miner says its pool is not on the network.
> Check the pool's name in the Mining Panel, and give it again with miner --pool.

> [!PROBLEM] A miner says it waits for the owner.
> The pool takes only the miners its owner accepts. The owner accepts it in the Settings tab, at the pool's own server.

> [!SEE] jsc:mining, jsc:network_services
