---
section: starting_a_computer
order: 7
icon: jsc:mainframe
---
# Starting through the network

From the Legacy era on, the firmware can start the installer of a system through the network, from the Mirror of the network's Mainframe ([](jsc:mirror)). You need no medium in a drive: the installer comes over the cable, and the install runs as it does from a medium.

Table: Network boot in each era

| Era | Where you find it | What it opens |
| --- | --- | --- |
| Vintage | Nowhere | A Vintage board has no network boot. |
| Legacy and Transition | Network (PXE), on the Boot page of the Setup | The network card's agent, then the Mirror's menu in the BIOS's blue. |
| Standard and Advanced | HTTP Boot, in the Boot Manager | The Mirror's list on the firmware's own page, with INSTALL and BACK. |

The menu lists only the systems this machine runs, for its era and its processor, each with its house. A system a player wrote shows its author and the word community. Boot from the local disk is always the last entry.

> [!DEFINE] PXE
> Preboot Execution Environment: the way a BIOS starts a program it gets from the network.

> [!DEFINE] Community
> What a system or a desktop a player wrote is marked with, beside its author.

## If something goes wrong

> [!PROBLEM] It says "No Mirror answered on the network".
> No Mainframe of this machine's network serves a Mirror. Connect the machine to the network, and turn the Mirror on in the Mainframe ([](jsc:mirror)).

> [!PROBLEM] A system is not in the list.
> It does not run on this machine. Check the system's era and processor against the machine's.

> [!SEE] jsc:boot_menu, jsc:player_systems, jsc:mirror
