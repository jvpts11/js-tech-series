---
section: quince_desktop_section
order: 358
---
# Memory on Quince's systems

## The classic systems: a block for each program

On the Quince System and Quince OS 9, each program you open gets a block of memory of its own, which it keeps while it runs. On the Quince System the block has one size. On Quince OS 9 two sizes set it, the preferred size and the minimum size: the program opens in its preferred size while that much is free, in what is left above its minimum otherwise, and not at all below its minimum.

> [!DEFINE] Preferred size
> The block a program opens in while that much memory is free.

> [!DEFINE] Minimum size
> The smallest block a program opens in on Quince OS 9.

With a program's window in front, choose Get Info from the File menu to change its sizes. The suggested size beside them is what the program's maker suggests. About This Computer, in the fruit's menu, shows each open program's block as a bar, the part in use darker.

> [!DEFINE] Get Info
> The window that shows what a file or program is, and sets a program's memory on a classic system.

## Quince OS X and quinceOS: Force Quit

The later systems share the memory as the other systems do. When it runs out, they do not end anything themselves: every program pauses, and Force Quit Applications asks which to quit. Choose one or more and press Force Quit. Once there is room, the programs go on.

> [!DEFINE] Force Quit Applications
> The window that asks which programs to quit when the memory runs out.

## If something goes wrong

> [!PROBLEM] A classic system says there is not enough memory to open a program.
> Quit a program to free its block, or lower the program's minimum size in Get Info.

> [!PROBLEM] Every program on Quince OS X stops moving.
> The memory ran out: choose what to quit in Force Quit Applications.

> [!SEE] jsc:quince_systems, jsc:quince_crashes
