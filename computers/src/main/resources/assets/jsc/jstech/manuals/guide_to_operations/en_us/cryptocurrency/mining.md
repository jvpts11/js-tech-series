---
section: cryptocurrency
order: 115
icon: jsc:mining_computer_standard
covers: [jsc:mining_computer_transition, jsc:mining_computer_standard, jsc:mining_computer_advanced, jsc:motherboard_mng_transition, jsc:motherboard_mng_standard, jsc:motherboard_mng_advanced, jsc:mining_riser_transition, jsc:mining_riser_standard, jsc:mining_riser_advanced, jsc:asic_forge_s1, jsc:asic_forge_a2]
---
# Mining: the miner, the Mining Computer and the ASICs

Mining is how TRC are made. A miner spends its hashrate on finding the next block, and the block's reward goes to the miner's payout address: the address of your wallet, from its Receive tab. Mining draws the watts of the parts that mine, which is its real cost.

> [!DEFINE] Hashrate
> How many hashes a second a miner tries. A processor mines at its capacity, a graphics card at ten times its power.

> [!DEFINE] Blockwright miner
> The program that mines on a computer, at its prompt or in the background.

## The miner at the prompt

Install the Blockwright miner, then give it your address: miner --address and the address. Type miner to watch it mine, and Q to stop. miner start mines in the background, whenever the computer is on, and miner stop ends it; miner status says how it goes. miner --pool and a pool's name mines for that pool, and miner --alone for you alone.

```command
id: jsc:command/mc_net/miner
```

## The Mining Computer

The Mining Computer is a mining rig: an open frame that holds a mining board, a processor, memory, one power supply, one disk and graphics cards on risers, all in view. The Transition's stands in one block and takes 4 cards; the Standard's and the Advanced's are two blocks wide and take 6 and 8. It is a real computer, with no monitor needed: you reach it by ssh ([](jsc:remote_access)) or watch it in the Mining Panel.

1. Place the Mining Computer and open it.
2. Put in the mining board of its era, a processor, memory, a power supply, a disk, and a riser for each card it will carry.
3. Switch it off. With a graphics card in your hand, aim at a free riser and use it to seat the card. Sneak with an empty hand to take a card out.
4. Install a system and the miner, set the payout address, and turn on Mine at start so it mines whenever it is on.

A riser takes the cards of its era or older. On a riser a card mines at its full power: mining ignores the slot's cut.

## The ASICs

An ASIC is a block made only to mine, by Blockwright. It has no system: its window sets its payout address and its pool. Give it energy and put it on the network with a data cable; it mines while it has its watts.

Table: What mines how much

| Miner | Hashrate | Draws |
| --- | --- | --- |
| A Transition rig of four cards | about 13,600 H/s | its parts' watts |
| A Standard rig of six cards | about 157,000 H/s | its parts' watts |
| An Advanced rig of eight cards | about 467,000 H/s | its parts' watts |
| Forge S1 (Standard) | 1,000,000 H/s | 1,400 W |
| Forge A2 (Advanced) | 10,000,000 H/s | 3,200 W |

## If something goes wrong

> [!PROBLEM] A card will not seat on a riser.
> Switch the rig off, aim at a free riser, and check the card is of the riser's era or older.

> [!PROBLEM] The miner says it has no address to pay.
> Give it one: miner --address and your wallet's address, or the address in the ASIC's window.

> [!PROBLEM] The miner says nothing on the machine hashes.
> Put in a processor, or seat graphics cards on the rig's risers.

> [!SEE] jsc:teracoin, jsc:mining_pools
