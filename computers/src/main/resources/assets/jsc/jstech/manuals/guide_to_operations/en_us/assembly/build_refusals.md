---
section: assembly
order: 17
---
# When the build is refused

When a build has problems, a rack server's assembly screen can tell you which ones. Its power area has a label, CHECKS. Hover it and every problem of the build shows as one line.

Towers and the Mainframe do not list the problems. Their POWER tooltip gives only the three generic lines of [The assembly screen](jsc:assembly_screen), so use the table below to find the cause.

When nothing is wrong, the list says "All checks passed" in green. With no board or power supply in place, it says "Insert a motherboard and PSU to begin".

> [!DEFINE] Power draw
> The watts the parts ask for in total. The processors, the memory, the disks and the expansion cards add up to it. The motherboard's own power is not counted.

> [!DEFINE] Expansion slot
> A slot on the board for an expansion card. The bus standards are ISA, PCI, AGP (Accelerated Graphics Port) and PCI-e (PCI Express), from the oldest to the newest.

> [!DEFINE] Bus family
> The family a bus belongs to: ISA, PCI, AGP or PCI-e. A card fits a board only when the bus families match, but for the PCI cards of the Workshop and the PCI Crafting Card, which fit any board but an ISA one. ISA is also the name of the oldest bus.

Table: The lines of the CHECKS list

| The line | What it means | The fix |
| --- | --- | --- |
| `no CPU installed` | The build has no processor. | Put a processor in the socket the board names. |
| `no RAM installed` | The build has no memory module. | Put in a module of a generation the board takes, of its era. |
| `too many CPUs: <installed> installed, <sockets> sockets` | More processors than the board has sockets for. | Take processors out, or use a board with more CPU slots. |
| `CPU socket <a> does not fit board socket <b>` | A processor's socket is not the board's socket. | Use a processor with the board's socket. |
| `CPU instruction set <a> does not match the <b> of the other processors` | Two processors understand different instruction sets. | Use processors of one instruction set. |
| `too many PCI-e cards: <installed> installed, <slots> PCI-e slots` | More expansion cards than the board has expansion slots. | Take cards out, or use a board with more PCI-e slots. |
| `expansion card bus family <a> is not compatible with board bus <b>` | A card's bus family is not the board's bus family. | Use a card of the board's bus family. |
| `a sound card of the <a> era does not sit on a board of the <b> era` | The sound card is of another era than the board. | Use a sound card of the board's era. |
| `one sound card per machine: <installed> installed` | The computer has two sound cards. | Take one of them out. |
| `too many RAM modules: <installed> installed, <slots> slots` | More memory modules than the board has slots. | Take modules out, or use a board with more RAM slots. |
| `RAM generation <a> not accepted by board` | A module's generation is not on the board's list. | Use a module of a generation the board takes. |
| `too many disks: <installed> installed, <slots> disk slots` | More disks in the board's disk slots than it has. | Move disks to the Hot-Swap Drive Bay, or use a board with more disk slots. |
| `PSU insufficient: draw <a>W exceeds <b>W` | The power draw is more than the power supply gives. | Use a power supply with more watts. On a rack server, the DRAW readout shows the total. |

The numbers in each line change to match your build.

## Parts that stay out of their slot

Some parts never go into a slot. Hold one over the slot, and the window says which rule keeps it out, as the table below lists them.

Table: Parts that stay out

| Part | Why it stays out |
| --- | --- |
| A board of another form factor or era | The case takes only its own form factors, and boards of its own era. |
| A processor of another socket or era, while a board is in place | It must match the board's socket and era. |
| A memory module of another generation or era, while a board is in place | It must match a generation the board takes, and the board's era. |
| A disk of a newer era than the computer | A disk fits the computer's own era or an older one. |
| A part other than a drive in a bay | A bay takes only a drive. |
| A second sound card | A computer takes one sound card. |
| A second Cluster Interface Card in a Cluster Management Computer | A Cluster Management Computer takes only one of them. |
| A Workshop card in a computer other than a Personal Computer | Only a Personal Computer takes Workshop cards. |
| An Integra Phi coprocessor in a rack server other than a supercomputer node | Only a supercomputer node takes one, and only one per node. |

A board the case refuses leaves the computer with no build at all. The same happens with no power supply in place.

## If something goes wrong

> [!PROBLEM] A part does not go into its slot.
> Hold it over the slot to read the rule it breaks, and find that rule in the tables above.

> [!PROBLEM] The server says "Insert a motherboard and PSU to begin".
> Put in a board the case takes, and a power supply.
