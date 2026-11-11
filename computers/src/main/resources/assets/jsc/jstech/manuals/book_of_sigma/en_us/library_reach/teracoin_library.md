---
section: library_reach
order: 469
---
# Teracoin: wallets, the chain, the miner and the pools

Your program can reach Teracoin: the balance of any address, the wallets in your computer's files, the chain, your computer's own miner and the Mining Pools of its network. You do it through Wallet, Chain, Miner and Pool, built-in types of System.Teracoin. They came with Sigma# 3.

Amounts are whole hundred-millionths of a TRC, as the chain keeps them: a TRC is 100000000 of them, and your program never rounds a coin away.

Table: Wallet and Chain members

| Member | What it does |
| --- | --- |
| `Wallet.Balance(address)` | What the address holds. |
| `Wallet.Address(path)` | The address of the wallet in that file, or empty text when the file holds none. |
| `Wallet.Send(path, to, amount, label)` | Sends the amount from the wallet in that file to the address, with the label, and gives yes; no when the wallet does not hold that much. |
| `Chain.Height` | How many blocks the chain has. |
| `Chain.Difficulty` | How hard the next block is to find. |
| `Chain.Reward` | What finding the next block pays. |

Table: Miner members

| Member | What it does |
| --- | --- |
| `Miner.Installed` | Yes when the miner is installed. |
| `Miner.Status` | What it is doing: off, alone, pool, no_address, no_pool, waiting_for_owner or nothing_hashes. |
| `Miner.Hashrate` | How many hashes a second it makes. |
| `Miner.Pool, Miner.Address` | The pool it mines for, empty when it mines alone, and the address it pays. |
| `Miner.Background` | Yes while it runs in the background. |
| `Miner.SetAddress(address), Miner.UsePool(name), Miner.MineAlone()` | Sets the address it pays, the pool it mines for (empty for the network's first), or mining alone. |
| `Miner.Start(), Miner.Stop()` | Runs it in the background, and stops it. Start gives no when it has no address to pay. |

Table: Pool members

| Member | What it does |
| --- | --- |
| `Pool.Names()` | The names of the Mining Pools on the network. |
| `Pool.Of(name)` | The pool of that name, the network's first for empty text, or null when there is none. |
| `Pool.Name, Pool.Host, Pool.FeePermille, Pool.Hashrate, Pool.Members, Pool.Found` | Its name, the server it runs on, its fee in thousandths, how many hashes a second its miners make, how many miners it has, and how many blocks it found. |

A wallet is its file: whoever reads the file can spend from it, as in the Wallet program. A file that holds no wallet, an address written wrong, an amount of nothing or a label of more than one line stops your program with an ArgumentException. What you send leaves the wallet at once and reaches the other with the next block.

Miner reads and sets your computer's miner as the miner command does, and what you set is kept with the computer.

```sgs Rent.sgs
string wallet = "Wallets/main.wallet";
if (Wallet.Balance(Wallet.Address(wallet)) > 500000000) {
    Wallet.Send(wallet, "trc1qexampleexampleexampleexample", 100000000, "Rent");
}
```

> [!DEFINE] Hundred-millionth
> The smallest amount of Teracoin: a TRC split into 100000000 parts, the way the chain counts every amount you hold.

## If something goes wrong

> [!PROBLEM] Miner.Start gives no.
> Your miner has no address to pay. Call Miner.SetAddress first.

> [!PROBLEM] Wallet.Send gives no.
> The wallet does not hold that much. Read Wallet.Balance first.

> [!SEE] jsc:teracoin, jsc:mining, jsc:mining_pools
