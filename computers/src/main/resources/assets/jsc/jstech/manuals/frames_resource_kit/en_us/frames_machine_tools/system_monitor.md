---
section: frames_machine_tools
order: 224
---
# The System Monitor

The System Monitor is a dashboard of this machine at a glance: its processor, its memory, its graphics, and a bar for every disk. It only reads the machine and shows it, so you cannot change anything in it.

Open it before you start a big program, to see whether the machine has memory to spare. The memory part shows the random-access memory (RAM) that programs hold, and how much is free.

## What you read

The processor load is how busy the processor is, in percent. Memory shows the memory held, out of the total in MB, and the free memory below it.

Graphics shows "no GPU" when the machine has no graphics processor (GPU). Otherwise it shows the video memory held, out of the total, and the load of the card in percent.

Storage draws a bar for each disk, and marks the system disk with the word (system). It also counts the programs installed on the machine.

Power shows what the parts draw against the power supply's rating and what the line gives in FE a tick, and Temperature the processor's heat against the 90 degrees Celsius where it starts to slow, each over a small graph. You pick the graphs' period in the menu beside Power: the last hour, the last day or the last 7 days of the game.

> [!DEFINE] Load
> How busy a part of the machine is, in percent.

> [!DEFINE] Held
> Memory that programs have taken for themselves. Free memory is what is left.

## If something goes wrong

> [!PROBLEM] A bar shows a figure a moment late.
> The window reads the machine again about every 40 frames of the screen. Wait a moment, then look again.

> [!SEE] jsc:task_manager, jsc:this_pc
