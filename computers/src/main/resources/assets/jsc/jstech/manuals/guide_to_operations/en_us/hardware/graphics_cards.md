---
section: hardware
order: 21
icon: jsc:gpu_vertex_gtx_970
covers_family: jsc:graphics_cards
---
# Graphics cards

The graphics card draws the picture: it lights the monitors and draws the windows on them. A computer with no graphics card has nothing to show, unless its processor has graphics built in.

A card has a memory of its own, the video memory (VRAM). Every monitor uses some of it, a bigger monitor more, and every window that draws a picture of its own uses some too.

> [!DEFINE] Video memory (VRAM)
> The graphics card's own memory. Monitors and the windows that draw pictures use it.

A processor with graphics built in has no video memory of its own. It takes a quarter of the computer's RAM for its picture, up to 1,792 MB. A computer with a graphics card of its own does not share its RAM this way.

In a Mainframe, each card adds one parallel queue: a line of work the Mainframe runs at the same time as the others. A Mainframe with three cards runs four queues, one from its processors and one for each card ([States, priorities and queues](jsc:states_and_queues)).

A card's queue never runs faster than the processors of the Mainframe. A card whose queue runs slower than that is shown in amber.

Cards go in the board's expansion slots ([Expansion cards](jsc:expansion_cards)). A card in an older slot runs slower than its tooltip says. The table shows how much of its speed it keeps.

Table: A card in an older slot

| Slot compared with the card | Speed the card keeps |
| --- | --- |
| The same generation or newer | All of it |
| One generation older | Half |
| Two generations older | A quarter |
| Three or more generations older | An eighth, which is the least it keeps |

The same share cuts the video memory the computer counts for the card, and the card's queue.

There are 76 cards, from the VGA-256 to the Envya Vertex RTX 5090. Hover a card and its tooltip shows you its cores, its video memory and its power.

## Video memory for the monitors

A monitor is counted in blocks of its big screen. Its video memory cost is its number of blocks, times the cost of one block in its era.

Table: Video memory for one block of a monitor

| Era | Video memory for one block |
| --- | --- |
| Vintage | 64 KB (monochrome) or 256 KB (16 colours) |
| Legacy | 4 MB |
| Transition | 16 MB |
| Standard | 64 MB |
| Advanced | 256 MB |
| Exa | 1 GB |
| Singularity | 4 GB |

A window that draws a picture costs a quarter of a colour block of its era, or a whole block while it is maximised. Only graphical programs take that memory, and a minimised window takes none.

## If something goes wrong

> [!PROBLEM] The monitor stays dark while the computer is off.
> Switch the computer on. Monitors light only while the computer runs.

> [!PROBLEM] The monitor stays dark.
> No graphics card is in the computer, and its processor has no graphics. Put in a card, or use a processor with graphics built in.

> [!PROBLEM] The monitor says "Not enough video memory: this monitor needs <a>, and <b> are free".
> Use a card with more video memory, or fewer or smaller monitors.

> [!PROBLEM] A card does less than its tooltip says.
> It sits in an older slot. Move it to a newer slot.

> [!SEE] jsc:monitors, jsc:expansion_cards, jsc:states_and_queues
