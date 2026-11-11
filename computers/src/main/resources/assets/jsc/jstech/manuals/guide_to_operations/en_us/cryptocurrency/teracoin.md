---
section: cryptocurrency
order: 114
icon: jsc:monitor
---
# Teracoin and the Crypto Wallet

Teracoin, TRC for short, is the cryptocurrency of the world: one chain of blocks for the whole server, owned by nobody. Miners find its blocks and are paid in TRC. You keep yours in a wallet, send them to other players, and turn them into items on a 3D printer.

> [!DEFINE] Teracoin
> The world's cryptocurrency, TRC. A TRC divides into eight decimal places.

> [!DEFINE] Wallet
> A file on a computer's disk that keeps the key of one address. Whoever has the file can spend what the address holds.

> [!DEFINE] Crypto Wallet
> Blockwright's program that makes wallets, shows their balance and their history, sends TRC and prints with them.

Install the Crypto Wallet on a computer of the Transition or later. The first time you open it, it offers to make a wallet: give it a name, and it saves the wallet's file in the Wallets folder of the computer's documents. With a USB stick in the computer, tick the box to save a copy on it as well.

The coins live on the chain, at the wallet's address, not in its file. Copying the file copies the access, not the coins: whoever has a copy spends the same balance. Lose every copy and the TRC are gone for good, so keep a copy somewhere safe.

Table: The wallet's tabs

| Tab | What it does |
| --- | --- |
| Overview | The balance, what waits for the next block, and the latest moves. |
| Send | Sends TRC to an address. A send has no fee: it waits for the next block, or ten minutes when no block comes. |
| Receive | The wallet's address, to copy, also drawn as a square of blocks to show across a room. |
| Transactions | Every move of the wallet: received, sent, paid by a pool and printed. |
| Print | Prints items on the 3D printers linked to the computer ([](jsc:printers_3d)). |
| Wallets | The wallets on this computer, to open one or make another. |

## Blocks, the difficulty and the reward

A block comes about every ten minutes. Each second, each miner's chance of finding it is its hashrate divided by the difficulty, which changes every ten blocks to keep that pace. A block pays 50 TRC, halving every 2,100 blocks, and one block in twenty also brings a rare item, such as a diamond, into the network storage of the machine that found it. Your server's settings may give other numbers.

## If something goes wrong

> [!PROBLEM] The wallet says its file no longer holds its key.
> The file is gone or was changed. Open another wallet in the Wallets tab, or copy the file back from where you kept it.

> [!PROBLEM] A send says that is not a Teracoin address.
> Copy the address again from the other wallet's Receive tab: it starts with trc1q.

> [!PROBLEM] A send waits a long time.
> It waits for the next block. When none comes for ten minutes, it goes through on its own.

> [!SEE] jsc:mining, jsc:mining_pools, jsc:printers_3d
