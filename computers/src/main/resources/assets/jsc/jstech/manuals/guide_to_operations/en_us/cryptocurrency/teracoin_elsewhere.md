---
section: cryptocurrency
order: 118
icon: jsc:asic_forge_s1
---
# Teracoin on a portable and in a program

Two more places hold Teracoin beside a computer's Crypto Wallet. A portable runs the Crypto Wallet too, from the Transition on ([](jsc:portables)), with one wallet kept on the device itself rather than in a file: you see its balance and its address, send from it, and read its latest movements. The device reaches the chain through the network it is joined to; without one, its wallet waits.

Your programs reach Teracoin through System.Teracoin, a part of the library of Sigma# 3 ([](jsc:teracoin_library)). A program reads a wallet file's address and balance and sends from it, reads the chain, sets its machine's miner as the miner command does, and lists the network's Mining Pools. A program that reads a wallet's file can spend from it, as you can, so keep a wallet's file where only your own programs run.

## If something goes wrong

> [!PROBLEM] The portable's wallet shows nothing new.
> The device is joined to no network, and its wallet waits for one. Join it to a network ([](jsc:portables)).

> [!PROBLEM] A program's send gives no.
> The wallet does not hold that much. Have the program read Wallet.Balance first ([](jsc:teracoin_library)).

> [!SEE] jsc:teracoin, jsc:portables, jsc:teracoin_library
